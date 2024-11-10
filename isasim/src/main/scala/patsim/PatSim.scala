// File: PatSim.scala
package patsim

import scala.collection.mutable
import patsim.Metrics._
import patsim.Opcode._
import patsim.Function._
import patsim.Constants._

/**
 * L1 Cache Implementation with Detailed Hit/Miss Statistics
 */
class L1Cache(cacheSize: Int = 16) { // 16 lines for simplicity
  // Each cache line holds a tag and data
  case class CacheLine(tag: Int, data: Int)

  // Initialize cache with invalid tags
  private val cache = Array.fill(cacheSize)(CacheLine(-1, 0))

  // Additional Metrics
  var readHits: Int = 0
  var readMisses: Int = 0
  var writeHits: Int = 0
  var writeMisses: Int = 0

  /**
   * Access the cache for a given address.
   * Returns (hit: Boolean, data: Int)
   */
  def access(address: Int, isLoad: Boolean, writeData: Int = 0): (Boolean, Int) = {
    val index = (address / 4) % cacheSize
    val tag = address / (4 * cacheSize)
    val line = cache(index)

    if (line.tag == tag) {
      if (isLoad) {
        readHits += 1
        Metrics.cacheHits += 1 // Existing total hits
        // Cache Hit: Return data
        println(s"[L1Cache] Load Hit at index $index for address $address")
        (true, line.data)
      } else {
        writeHits += 1
        Metrics.cacheHits += 1 // Existing total hits
        // Store Hit: Update data
        cache(index) = CacheLine(tag, writeData)
        println(s"[L1Cache] Store Hit at index $index for address $address with data $writeData")
        (true, 0)
      }
    } else {
      if (isLoad) {
        readMisses += 1
        Metrics.cacheMisses += 1 // Existing total misses
        // Cache Miss: For simulation, assume data is 0
        println(s"[L1Cache] Load Miss at index $index for address $address. Loading data=0")
        cache(index) = CacheLine(tag, 0) // Load data from memory (simulated as 0)
        (false, 0)
      } else {
        writeMisses += 1
        Metrics.cacheMisses += 1 // Existing total misses
        // Cache Miss: For store, write directly to memory (simulated)
        println(s"[L1Cache] Store Miss at index $index for address $address. Writing data=$writeData to memory")
        (false, 0)
      }
    }
  }

  /**
   * Print Detailed Cache Metrics
   */
  def printCacheMetrics(): Unit = {
    println("\n=== L1 Cache Detailed Metrics ===")
    println(s"Read Hits: $readHits")
    println(s"Read Misses: $readMisses")
    println(s"Write Hits: $writeHits")
    println(s"Write Misses: $writeMisses")
    println(f"Read Hit Rate: ${if (readHits + readMisses > 0) (readHits.toDouble / (readHits + readMisses)) * 100 else 0}%.2f%%")
    println(f"Write Hit Rate: ${if (writeHits + writeMisses > 0) (writeHits.toDouble / (writeHits + writeMisses)) * 100 else 0}%.2f%%")
    println("==============================\n")
  }
}

/**
 * Case Class for Decoded Instructions
 */
case class DecodedInstruction(
                               pred: Int = 0,
                               opcode: Int = 0,
                               rd: Int = 0,
                               rs1: Int = 0,
                               rs2: Int = 0,
                               opc: Int = 0,
                               imm12: Int = 0,
                               func: Int = 0,
                               op1: Int = 0,
                               op2: Int = 0,
                               isNop: Boolean = true,
                               isCompress: Boolean = false,
                               isDecompress: Boolean = false,
                               isMul: Boolean = false,      // Flag for MUL
                               isMulu: Boolean = false,     // Flag for MULU
                               result: Int = 0
                             )

/**
 * Pipeline Register Structures
 */
case class EX_MEM(
                   memDataA: Int = 0,
                   rdA: Int = 0,
                   validA: Boolean = false,
                   memDataB: Int = 0,
                   rdB: Int = 0,
                   validB: Boolean = false,
                   decodedA: DecodedInstruction = DecodedInstruction(),
                   decodedB: DecodedInstruction = DecodedInstruction()
                 )

case class MEM_WB(
                   memDataA: Int = 0,
                   rdA: Int = 0,
                   validA: Boolean = false,
                   memDataB: Int = 0,
                   rdB: Int = 0,
                   validB: Boolean = false
                 )

case class DE_EX(
                  decodedA: DecodedInstruction = DecodedInstruction(),
                  decodedB: DecodedInstruction = DecodedInstruction()
                )

case class COMPRESS_STAGE(
                           compressResultA: Int = 0,
                           rdA: Int = 0,
                           validA: Boolean = false,
                           compressResultB: Int = 0,
                           rdB: Int = 0,
                           validB: Boolean = false
                         )

/**
 * Main Pipeline Simulator Class
 */
class PatSim(instructions: Array[Int]) {
  // Register File (R0-R31), R0 is always 0
  val reg: Array[Int] = Array.fill(32)(0)

  // Program Counter
  var pc: Int = 0

  // Pipeline Registers
  var de_ex: DE_EX = DE_EX()
  var ex_mem: EX_MEM = EX_MEM()
  var mem_wb: MEM_WB = MEM_WB()
  var ex_wb: MEM_WB = MEM_WB()

  // Compression Stage
  var compress_stage: COMPRESS_STAGE = COMPRESS_STAGE()

  // Instantiate L1 Cache
  val l1Cache = new L1Cache()

  // Simulation Control
  var halt: Boolean = false
  var stall: Boolean = false
  var flush: Boolean = false

  /**
   * Fetch Stage
   */
  def fetchStage(): Unit = {
    if (pc < instructions.length) {
      val instrA = instructions(pc)
      val instrB = if (pc + 1 < instructions.length) instructions(pc + 1) else NOP
      println(s"[Fetch] Fetched instructions at PC=$pc: A=0x${instrA.toHexString}, B=0x${instrB.toHexString}")
      de_ex = DE_EX(
        decodedA = decodeInstruction(instrA),
        decodedB = decodeInstruction(instrB)
      )
      pc += 2
    } else {
      de_ex = DE_EX() // Insert NOPs if no instructions left
      halt = true
    }
  }

  /**
   * Decode Stage with Hazard Detection
   */
  def decodeStageWithHazardDetection(): Unit = {
    val decodedA = de_ex.decodedA
    val decodedB = de_ex.decodedB

    // Collect source and destination registers from current instructions
    val srcRegsA = Set(decodedA.rs1, decodedA.rs2).filter(_ != 0)
    val srcRegsB = Set(decodedB.rs1, decodedB.rs2).filter(_ != 0)
    val srcRegs = srcRegsA ++ srcRegsB

    // Collect destination registers from EX_MEM, MEM_WB, EX_WB
    val destRegs = Set(
      ex_mem.rdA,
      ex_mem.rdB,
      mem_wb.rdA,
      mem_wb.rdB,
      ex_wb.rdA,
      ex_wb.rdB
    ).filter(_ != 0)

    // Detect RAW hazards
    val rawHazard = srcRegs.intersect(destRegs).nonEmpty

    // Detect WAW hazards
    val wawHazard = (decodedA.rd == decodedB.rd) && (decodedA.rd != 0)

    // Combine hazard conditions
    val hazard = rawHazard || wawHazard

    if (hazard) {
      println("[Hazard Detection] Data hazard detected. Applying forwarding or stalling.")
      // Determine if forwarding can resolve the hazard
      val canForward = checkForwardingCapability(decodedA, decodedB)

      if (!canForward) {
        // Introduce a stall by inserting a NOP in the pipeline
        println("[Hazard Detection] Unable to resolve hazard with forwarding. Introducing stall.")
        stall = true
        // Insert NOP by keeping DE_EX unchanged and preventing IF_DE from advancing
      } else {
        // Forwarding resolves the hazard; no stall needed
        println("[Hazard Detection] Hazard resolved by forwarding.")
        stall = false
        decodeStage()
      }
    } else {
      stall = false
      // Proceed with normal decode
      decodeStage()
    }
  }

  /**
   * Check if forwarding can resolve the hazard
   */
  def checkForwardingCapability(decodedA: DecodedInstruction, decodedB: DecodedInstruction): Boolean = {
    // Simplified forwarding capability check
    true // Assume forwarding can always resolve for this simulation
  }

  /**
   * Decode Stage Execution
   */
  def decodeStage(): Unit = {
    // Already handled in fetchStage and hazard detection
    // Additional decode logic can be added here if necessary
  }

  /**
   * Execute Stage
   */
  def executeStage(): Unit = {
    val decodedA = de_ex.decodedA
    val decodedB = de_ex.decodedB

    // Execute Instruction A
    val (memDataA, rdA, validA) = execute(decodedA)

    // Execute Instruction B
    val (memDataB, rdB, validB) = execute(decodedB)

    // Update EX_MEM pipeline register
    ex_mem = EX_MEM(
      memDataA = memDataA,
      rdA = rdA,
      validA = validA,
      memDataB = memDataB,
      rdB = rdB,
      validB = validB,
      decodedA = decodedA,
      decodedB = decodedB
    )

    println(s"[Execute] Instruction A: Result=$memDataA, RD=R$rdA, Valid=$validA")
    println(s"[Execute] Instruction B: Result=$memDataB, RD=R$rdB, Valid=$validB")

    // Store results in DE_EX for forwarding
    de_ex = de_ex.copy(
      decodedA = decodedA.copy(result = memDataA),
      decodedB = decodedB.copy(result = memDataB)
    )
  }

  /**
   * ALU Operations
   */
  def alu(func: Int, op1: Int, op2: Int): Int = {
    func match {
      case ADD => op1 + op2
      case SUB => op1 - op2
      case XOR => op1 ^ op2
      case SL => op1 << (op2 & 0x1f)
      case SR => op1 >>> (op2 & 0x1f)
      case SRA => op1 >> (op2 & 0x1f)
      case OR => op1 | op2
      case AND => op1 & op2
      case NOR => ~(op1 | op2)
      case SHADD => (op1 << 1) + op2
      case SHADD2 => (op1 << 2) + op2
      case _ => 0 // Default case
    }
  }

  /**
   * Execute Instruction Logic
   */
  def execute(decoded: DecodedInstruction): (Int, Int, Boolean) = {
    if (decoded.isNop) {
      // No operation, return defaults
      (0, 0, false)
    } else {
      Metrics.totalInstructions += 1 // Increment instruction count

      val op1 = decoded.op1
      val op2 = decoded.op2

      // Handle ALUm Instruction
      if (decoded.isMul || decoded.isMulu) {
        // Validate function code
        if (decoded.func != MUL && decoded.func != MULU) {
          println(s"[Execute] ALUm Error: Invalid function code ${decoded.func}. Inserting NOP.")
          Metrics.branchMispredictions += 1 // Placeholder for error tracking
          return (0, 0, false)
        }

        // Perform multiplication
        println(s"[Execute] Handling ALUm Instruction: R${decoded.rd} = R${decoded.rs1} * R${decoded.rs2}")
        val product = if (decoded.isMulu) {
          (op1.toLong & 0xFFFFFFFFL) * (op2.toLong & 0xFFFFFFFFL) // Unsigned multiply
        } else {
          op1.toLong * op2.toLong // Signed multiply
        }
        val sl = (product & 0xFFFFFFFFL).toInt // Lower 32 bits
        val sh = (product >>> 32).toInt // Higher 32 bits

        // Store sl in rd and sh in rd + 1
        if (decoded.rd < 31) { // Ensure rd + 1 does not exceed register file
          reg(decoded.rd) = sl
          reg(decoded.rd + 1) = sh
          println(s"[Execute] ALUm: R${decoded.rd} = $sl (sl), R${decoded.rd + 1} = $sh (sh)")
          if (decoded.isMulu) {
            Metrics.muluInstructions += 1
          } else {
            Metrics.mulInstructions += 1
          }
          (sl, sh, true)
        } else {
          println(s"[Execute] ALUm Error: rd=${decoded.rd} is the last register. Cannot store sh in rd+1.")
          Metrics.branchMispredictions += 1 // Placeholder for error tracking
          (0, 0, false)
        }
      }
      // Handle Compression Instructions
      else if (decoded.isCompress || decoded.isDecompress) {
        // Handle Compression Instructions
        println(s"[Execute] Handling ${if (decoded.isCompress) "COMPRESS" else "DECOMPRESS"} Instruction")
        // Pass operands to Compress Stage
        if (decoded.isCompress) {
          compress_stage = compress_stage.copy(
            compressResultA = decoded.op1,
            rdA = decoded.rd,
            validA = true
          )
        }
        if (decoded.isDecompress) {
          compress_stage = compress_stage.copy(
            compressResultB = decoded.op1,
            rdB = decoded.rd,
            validB = true
          )
        }
        (0, 0, false) // No further action needed in Execute Stage
      }
      // Handle Load Typed (Ldt) Instruction
      else if (decoded.opcode == Ldt) {
        val address = op1 + (decoded.imm12 << 2) // Example address calculation based on type
        println(s"[Execute] Load Typed: Calculating address R${decoded.rs1} + (${decoded.imm12} << 2) = $address")
        // Pass address to MEM stage
        (address, decoded.rd, true) // Result holds the memory address
      }
      // Handle Store Typed (Stt) Instruction
      else if (decoded.opcode == Stt) {
        val address = op1 + (decoded.imm12 << 2) // Example address calculation based on type
        val data = op2 // Data to store
        println(s"[Execute] Store Typed: Calculating address R${decoded.rs1} + (${decoded.imm12} << 2) = $address with data R${decoded.rs2} = $data")
        // Pass address and data to MEM stage
        (address, decoded.rd, true) // Result holds the memory address
      }
      else {
        // Handle other ALU operations
        val aluResult = alu(decoded.func, op1, op2)
        val rd = decoded.rd

        // Handle branch instructions
        if (decoded.opcode == Branch || decoded.opcode == BranchCf) {
          // Simple branch condition: if op1 is zero, branch is taken
          if (op1 == 0) {
            println(s"[Execute] Branch taken. Flushing pipeline.")
            flush = true
            Metrics.branchMispredictions += 1 // Increment branch mispredictions
            // Update PC to branch target (for simplicity, assume imm12 is the target offset)
            pc = pc + decoded.imm12
          } else {
            println(s"[Execute] Branch not taken.")
          }
          // Branch instructions do not write back to registers
          (0, 0, false)
        }
        else {
          // Write the ALU result to rd
          if (rd != 0) { // R0 is always zero
            reg(rd) = aluResult
            println(s"[Execute] ALU Operation: R$rd = $aluResult")
          }
          (aluResult, rd, true)
        }
      }
    }

    /**
     * Compress Stage Execution
     */
    def compressStage(): Unit = {
      val compressA = compress_stage.compressResultA
      val rdA = compress_stage.rdA
      val validA = compress_stage.validA

      val compressB = compress_stage.compressResultB
      val rdB = compress_stage.rdB
      val validB = compress_stage.validB

      // Handle COMPRESS Instruction
      if (validA && rdA != 0) {
        Metrics.compressInstructions += 1 // Increment COMPRESS count
        // Enhanced Compression Logic: Rotate Left by 4 bits
        val compressedData = compressData(compressA)
        println(s"[Compress Stage] COMPRESS: R$rdA compressed data = $compressedData")
        // Forward the compressed data to the next pipeline stage (e.g., Write Back)
        ex_wb = ex_wb.copy(
          memDataA = compressedData,
          rdA = rdA,
          validA = true
        )
      }

      // Handle DECOMPRESS Instruction
      if (validB && rdB != 0) {
        Metrics.decompressInstructions += 1 // Increment DECOMPRESS count
        // Enhanced Decompression Logic: Rotate Right by 4 bits
        val decompressedData = decompressData(compressB)
        println(s"[Compress Stage] DECOMPRESS: R$rdB decompressed data = $decompressedData")
        // Forward the decompressed data to the next pipeline stage (e.g., Write Back)
        ex_wb = ex_wb.copy(
          memDataB = decompressedData,
          rdB = rdB,
          validB = true
        )
      }

      // Reset COMPRESS_STAGE after processing
      compress_stage = COMPRESS_STAGE()
    }

    /**
     * Simple Compression Logic: Rotate Left by 4 bits
     */
    def compressData(data: Int): Int = {
      Integer.rotateLeft(data, 4)
    }

    /**
     * Simple Decompression Logic: Rotate Right by 4 bits
     */
    def decompressData(data: Int): Int = {
      Integer.rotateRight(data, 4)
    }

    /**
     * Write Back Stage
     */
    def writeBack(): Unit = {
      // Handle Write Back for Instruction A
      if (mem_wb.validA && mem_wb.rdA != 0) {
        reg(mem_wb.rdA) = mem_wb.memDataA
        println(s"[Write Back] Register R${mem_wb.rdA} updated to ${mem_wb.memDataA} from MEM_WB")
      }

      // Handle Write Back for Instruction B
      if (mem_wb.validB && mem_wb.rdB != 0) {
        reg(mem_wb.rdB) = mem_wb.memDataB
        println(s"[Write Back] Register R${mem_wb.rdB} updated to ${mem_wb.memDataB} from MEM_WB")
      }

      // Update MEM_WB from EX_MEM
      mem_wb = MEM_WB(
        memDataA = ex_mem.memDataA,
        rdA = ex_mem.rdA,
        validA = ex_mem.validA,
        memDataB = ex_mem.memDataB,
        rdB = ex_mem.rdB,
        validB = ex_mem.validB
      )
    }

    /**
     * Forwarding Unit
     */
    def ForwardingUnit(rs: Int): Int = {
      // Forwarding priority: EX_WB > MEM_WB > EX_MEM > DE_EX
      // Check EX_WB first
      if (rs == ex_wb.rdA && ex_wb.validA) {
        println(s"[Forwarding Unit] Forwarding from EX/WB Stage: R$rs = ${ex_wb.memDataA}")
        ex_wb.memDataA
      }
      else if (rs == ex_wb.rdB && ex_wb.validB) {
        println(s"[Forwarding Unit] Forwarding from EX/WB Stage: R$rs = ${ex_wb.memDataB}")
        ex_wb.memDataB
      }
      // Then check MEM_WB
      else if (rs == mem_wb.rdA && mem_wb.validA) {
        println(s"[Forwarding Unit] Forwarding from MEM/WB Stage: R$rs = ${mem_wb.memDataA}")
        mem_wb.memDataA
      }
      else if (rs == mem_wb.rdB && mem_wb.validB) {
        println(s"[Forwarding Unit] Forwarding from MEM/WB Stage: R$rs = ${mem_wb.memDataB}")
        mem_wb.memDataB
      }
      // Then check EX_MEM
      else if (rs == ex_mem.rdA && ex_mem.validA) {
        println(s"[Forwarding Unit] Forwarding from EX/MEM Stage: R$rs = ${ex_mem.memDataA}")
        ex_mem.memDataA
      }
      else if (rs == ex_mem.rdB && ex_mem.validB) {
        println(s"[Forwarding Unit] Forwarding from EX/MEM Stage: R$rs = ${ex_mem.memDataB}")
        ex_mem.memDataB
      }
      // Finally, check DE_EX (for newer instructions in the pipeline)
      else if (rs == de_ex.decodedA.rd && de_ex.decodedA.result != 0 && !de_ex.decodedA.isNop) {
        println(s"[Forwarding Unit] Forwarding from DE/EX Stage: R$rs = ${de_ex.decodedA.result}")
        de_ex.decodedA.result
      }
      else if (rs == de_ex.decodedB.rd && de_ex.decodedB.result != 0 && !de_ex.decodedB.isNop) {
        println(s"[Forwarding Unit] Forwarding from DE/EX Stage: R$rs = ${de_ex.decodedB.result}")
        de_ex.decodedB.result
      }
      // Default to Register File
      else {
        println(s"[Forwarding Unit] No forwarding required for R$rs. Using Register File: R$rs = ${reg(rs)}")
        reg(rs)
      }
    }

    /**
     * Decode Instruction Logic
     */
    def decodeInstruction(instr: Int): DecodedInstruction = {
      if (instr == NOP) {
        DecodedInstruction(isNop = true)
      } else {
        val pred = (instr >> 27) & 0x0F // 4 bits for Pred (bits 30-27)
        val opcode = (instr >> 22) & 0x1F // 5 bits (bits 26-22)
        val rd = (instr >> 17) & 0x1F
        val rs1 = (instr >> 12) & 0x1F
        val rs2 = (instr >> 7) & 0x1F
        val fixed_bits = (instr >> 4) & 0x07 // 3 bits (bits 6-4)
        val func = instr & 0x0F // 4 bits (bits 3-0)

        // *** Forwarding Logic ***
        // Fetch operands using forwarding
        val op1 = ForwardingUnit(rs1)
        val op2 = ForwardingUnit(rs2)
        // *** End Forwarding Logic ***

        // Debugging Logs
        println(s"\n[Decode] Decoding Instruction: 0x${instr.toHexString}")
        println(s"[Decode] Decoded Fields:")
        println(f"  pred: $pred%04d")
        println(f"  opcode: $opcode%05d")
        println(f"  rd: R$rd%02d")
        println(f"  rs1: R$rs1%02d")
        println(f"  rs2: R$rs2%02d")
        println(f"  fixed_bits: $fixed_bits%03d")
        println(f"  func: $func%04d")
        println(s"[Decode] Operands:")
        println(f"  op1 (R$rs1): $op1")
        println(f"  op2 (R$rs2): $op2")

        // Flags for new instructions
        val isCompress = opcode == Compress
        val isDecompress = opcode == Decompress
        val isMul = opcode == Alu && fixed_bits == 0x2 // ALUm opcode with fixed bits '010'

        DecodedInstruction(
          pred = pred,
          opcode = opcode,
          rd = rd,
          rs1 = rs1,
          rs2 = rs2,
          opc = 0, // Not used in ALUm; set to 0
          imm12 = 0, // Not used in ALUm; set to 0
          func = func,
          op1 = op1,
          op2 = op2,
          isNop = false,
          isCompress = isCompress,
          isDecompress = isDecompress,
          isMul = isMul && func == MUL,
          isMulu = isMul && func == MULU,
          result = 0 // To be updated in execute stage
        )
      }

      /**
       * MEM Stage Execution
       */
      def memStage(): Unit = {
        val memDataA = ex_mem.memDataA
        val rdA = ex_mem.rdA
        val validA = ex_mem.validA

        val memDataB = ex_mem.memDataB
        val rdB = ex_mem.rdB
        val validB = ex_mem.validB

        // Handle Load Typed (Ldt)
        if (rdA != 0 && validA) {
          val (hit, data) = l1Cache.access(memDataA, isLoad = true)
          if (hit) {
            // Data is already loaded via cache
          } else {
            // On miss, data is loaded as 0 (simulated)
          }
          println(s"[MEM Stage] Load Typed: Loaded data=$data from address $memDataA into R$rdA")
          mem_wb = mem_wb.copy(
            memDataA = data,
            rdA = rdA,
            validA = true
          )
        }

        // Handle Store Typed (Stt)
        if (rdB != 0 && validB) {
          val dataToStore = reg(rdB)
          l1Cache.access(memDataB, isLoad = false, writeData = dataToStore)
          println(s"[MEM Stage] Store Typed: Stored data=$dataToStore to address $memDataB from R$rdB")
          mem_wb = mem_wb.copy(
            memDataB = dataToStore,
            rdB = rdB,
            validB = true
          )
        }
      }

      /**
       * Write Back Stage
       */
      def writeBack(): Unit = {
        // Handle Write Back for Instruction A
        if (mem_wb.validA && mem_wb.rdA != 0) {
          reg(mem_wb.rdA) = mem_wb.memDataA
          println(s"[Write Back] Register R${mem_wb.rdA} updated to ${mem_wb.memDataA} from MEM_WB")
        }

        // Handle Write Back for Instruction B
        if (mem_wb.validB && mem_wb.rdB != 0) {
          reg(mem_wb.rdB) = mem_wb.memDataB
          println(s"[Write Back] Register R${mem_wb.rdB} updated to ${mem_wb.memDataB} from MEM_WB")
        }

        // Update MEM_WB from EX_MEM
        mem_wb = MEM_WB(
          memDataA = ex_mem.memDataA,
          rdA = ex_mem.rdA,
          validA = ex_mem.validA,
          memDataB = ex_mem.memDataB,
          rdB = ex_mem.rdB,
          validB = ex_mem.validB
        )
      }

      /**
       * Forwarding Unit
       */
      def ForwardingUnit(rs: Int): Int = {
        // Forwarding priority: EX_WB > MEM_WB > EX_MEM > DE_EX
        // Check EX_WB first
        if (rs == ex_wb.rdA && ex_wb.validA) {
          println(s"[Forwarding Unit] Forwarding from EX/WB Stage: R$rs = ${ex_wb.memDataA}")
          ex_wb.memDataA
        }
        else if (rs == ex_wb.rdB && ex_wb.validB) {
          println(s"[Forwarding Unit] Forwarding from EX/WB Stage: R$rs = ${ex_wb.memDataB}")
          ex_wb.memDataB
        }
        // Then check MEM_WB
        else if (rs == mem_wb.rdA && mem_wb.validA) {
          println(s"[Forwarding Unit] Forwarding from MEM/WB Stage: R$rs = ${mem_wb.memDataA}")
          mem_wb.memDataA
        }
        else if (rs == mem_wb.rdB && mem_wb.validB) {
          println(s"[Forwarding Unit] Forwarding from MEM/WB Stage: R$rs = ${mem_wb.memDataB}")
          mem_wb.memDataB
        }
        // Then check EX_MEM
        else if (rs == ex_mem.rdA && ex_mem.validA) {
          println(s"[Forwarding Unit] Forwarding from EX/MEM Stage: R$rs = ${ex_mem.memDataA}")
          ex_mem.memDataA
        }
        else if (rs == ex_mem.rdB && ex_mem.validB) {
          println(s"[Forwarding Unit] Forwarding from EX/MEM Stage: R$rs = ${ex_mem.memDataB}")
          ex_mem.memDataB
        }
        // Finally, check DE_EX (for newer instructions in the pipeline)
        else if (rs == de_ex.decodedA.rd && de_ex.decodedA.result != 0 && !de_ex.decodedA.isNop) {
          println(s"[Forwarding Unit] Forwarding from DE/EX Stage: R$rs = ${de_ex.decodedA.result}")
          de_ex.decodedA.result
        }
        else if (rs == de_ex.decodedB.rd && de_ex.decodedB.result != 0 && !de_ex.decodedB.isNop) {
          println(s"[Forwarding Unit] Forwarding from DE/EX Stage: R$rs = ${de_ex.decodedB.result}")
          de_ex.decodedB.result
        }
        // Default to Register File
        else {
          println(s"[Forwarding Unit] No forwarding required for R$rs. Using Register File: R$rs = ${reg(rs)}")
          reg(rs)
        }
      }

      /**
       * Decode Instruction Logic
       */
      def decodeInstruction(instr: Int): DecodedInstruction = {
        if (instr == NOP) {
          DecodedInstruction(isNop = true)
        } else {
          val pred = (instr >> 27) & 0x0F // 4 bits for Pred (bits 30-27)
          val opcode = (instr >> 22) & 0x1F // 5 bits (bits 26-22)
          val rd = (instr >> 17) & 0x1F
          val rs1 = (instr >> 12) & 0x1F
          val rs2 = (instr >> 7) & 0x1F
          val fixed_bits = (instr >> 4) & 0x07 // 3 bits (bits 6-4)
          val func = instr & 0x0F // 4 bits (bits 3-0)

          // *** Forwarding Logic ***
          // Fetch operands using forwarding
          val op1 = ForwardingUnit(rs1)
          val op2 = ForwardingUnit(rs2)
          // *** End Forwarding Logic ***

          // Debugging Logs
          println(s"\n[Decode] Decoding Instruction: 0x${instr.toHexString}")
          println(s"[Decode] Decoded Fields:")
          println(f"  pred: $pred%04d")
          println(f"  opcode: $opcode%05d")
          println(f"  rd: R$rd%02d")
          println(f"  rs1: R$rs1%02d")
          println(f"  rs2: R$rs2%02d")
          println(f"  fixed_bits: $fixed_bits%03d")
          println(f"  func: $func%04d")
          println(s"[Decode] Operands:")
          println(f"  op1 (R$rs1): $op1")
          println(f"  op2 (R$rs2): $op2")

          // Flags for new instructions
          val isCompress = opcode == Compress
          val isDecompress = opcode == Decompress
          val isMul = opcode == Alu && fixed_bits == 0x2 // ALUm opcode with fixed bits '010'

          DecodedInstruction(
            pred = pred,
            opcode = opcode,
            rd = rd,
            rs1 = rs1,
            rs2 = rs2,
            opc = 0, // Not used in ALUm; set to 0
            imm12 = 0, // Not used in ALUm; set to 0
            func = func,
            op1 = op1,
            op2 = op2,
            isNop = false,
            isCompress = isCompress,
            isDecompress = isDecompress,
            isMul = isMul && func == MUL,
            isMulu = isMul && func == MULU,
            result = 0 // To be updated in execute stage
          )
        }

        /**
         * MEM Stage Execution
         */
        def memStage(): Unit = {
          val memDataA = ex_mem.memDataA
          val rdA = ex_mem.rdA
          val validA = ex_mem.validA

          val memDataB = ex_mem.memDataB
          val rdB = ex_mem.rdB
          val validB = ex_mem.validB

          // Handle Load Typed (Ldt)
          if (rdA != 0 && validA) {
            val (hit, data) = l1Cache.access(memDataA, isLoad = true)
            if (hit) {
              // Data is already loaded via cache
            } else {
              // On miss, data is loaded as 0 (simulated)
            }
            println(s"[MEM Stage] Load Typed: Loaded data=$data from address $memDataA into R$rdA")
            mem_wb = mem_wb.copy(
              memDataA = data,
              rdA = rdA,
              validA = true
            )
          }

          // Handle Store Typed (Stt)
          if (rdB != 0 && validB) {
            val dataToStore = reg(rdB)
            l1Cache.access(memDataB, isLoad = false, writeData = dataToStore)
            println(s"[MEM Stage] Store Typed: Stored data=$dataToStore to address $memDataB from R$rdB")
            mem_wb = mem_wb.copy(
              memDataB = dataToStore,
              rdB = rdB,
              validB = true
            )
          }
        }

        /**
         * Tick Function
         */
        def tick(): Unit = {
          Metrics.totalCycles += 1
          println(s"\n=== Tick ${Metrics.totalCycles} ===")

          // Write Back Stage
          writeBack()

          // MEM Stage
          memStage()

          // Execute Stage
          executeStage()

          // Compress Stage
          compressStage()

          // Decode Stage with Hazard Detection
          if (!stall) {
            decodeStageWithHazardDetection()
          } else {
            println("[Tick] Pipeline is stalled. Inserting NOPs.")
            // Insert NOPs by keeping DE_EX unchanged
            de_ex = DE_EX()
            stall = false // Remove stall after inserting NOP
          }

          // Instruction Fetch Stage
          if (!stall) {
            fetchStage()
          } else {
            println("[Tick] Fetch stage is stalled. Not fetching new instructions.")
            // Optionally, you can insert NOPs or handle accordingly
          }

          // Check for halt condition
          if (halt) {
            println("Halt detected. Stopping simulation.")
          } else {
            // Log the state after each tick
            log()
          }
        }

        /**
         * Log Register States
         */
        def log(): Unit = {
          println(s"[Log] PC=$pc - Registers:")
          for (i <- 0 until reg.length) {
            print(s"R$i=${reg(i)} ")
          }
          println()
        }
      }

      /**
       * Companion Object with Main Method
       */
      object PatSimApp {

        def main(args: Array[String]): Unit = {
          println("Simulating Patmos with Enhanced Pipeline and L1 Cache")
          if (args.length != 1)
            throw new Error("Wrong Arguments, usage: PatSimApp <binary_input_file>")
          val instr = readBin(args(0))
          val simulator = new PatSim(instr)
          println("Patmos start")
          while (!simulator.halt) {
            simulator.tick()
          }
          println("Simulation completed")
          Metrics.printMetrics() // Print performance metrics
          simulator.l1Cache.printCacheMetrics() // Print detailed cache metrics
        }

        /**
         * Read a binary file into an Array
         * Reads binary data and assembles 4-byte words into Ints (big-endian)
         */
        def readBin(fileName: String): Array[Int] = {

          println("Reading " + fileName)
          // Open the file as a binary input stream
          val inputStream = new java.io.FileInputStream(fileName)
          val byteArray = new Array[Byte](inputStream.available())
          inputStream.read(byteArray)
          inputStream.close()

          // Convert bytes to Ints (4 bytes per Int, big-endian)
          val numInts = byteArray.length / 4
          val arr = new Array[Int](numInts)
          for (i <- 0 until numInts) {
            val byte0 = byteArray(i * 4) & 0xFF
            val byte1 = byteArray(i * 4 + 1) & 0xFF
            val byte2 = byteArray(i * 4 + 2) & 0xFF
            val byte3 = byteArray(i * 4 + 3) & 0xFF
            arr(i) = (byte0 << 24) | (byte1 << 16) | (byte2 << 8) | byte3
          }

          // Ensure at least one instruction
          if (arr.isEmpty) {
            Array(NOP)
          } else {
            arr
          }
        }
      }
    }
  }
}