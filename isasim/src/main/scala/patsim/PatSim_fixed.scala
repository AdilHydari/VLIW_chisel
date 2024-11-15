// File: PatSim.scala
package patsim

import scala.collection.mutable
import patsim.Metrics._
import patsim.Opcode._
import patsim.Function._
import patsim.Constants._

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
   * Main Pipeline Tick
   */
  def tick(): Unit = {
    if (!stall) {
      // Execute pipeline stages in reverse order to prevent data loss
      writebackStage()
      memoryStage()
      executeStage()
      compressStage()  // Execute compression stage before decode
      decodeStageWithHazardDetection()
      fetchStage()
      
      totalCycles += 1
    }
  }

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

    // Collect source and destination registers
    val srcRegsA = Set(decodedA.rs1, decodedA.rs2).filter(_ != 0)
    val srcRegsB = Set(decodedB.rs1, decodedB.rs2).filter(_ != 0)
    val srcRegs = srcRegsA ++ srcRegsB

    val destRegs = Set(
      ex_mem.rdA,
      ex_mem.rdB,
      mem_wb.rdA,
      mem_wb.rdB,
      ex_wb.rdA,
      ex_wb.rdB
    ).filter(_ != 0)

    // Detect hazards
    val rawHazard = srcRegs.intersect(destRegs).nonEmpty
    val wawHazard = (decodedA.rd == decodedB.rd) && (decodedA.rd != 0)
    val hazard = rawHazard || wawHazard

    if (hazard) {
      println("[Hazard Detection] Data hazard detected. Applying forwarding or stalling.")
      val canForward = checkForwardingCapability(decodedA, decodedB)

      if (!canForward) {
        println("[Hazard Detection] Unable to resolve hazard with forwarding. Introducing stall.")
        stall = true
      } else {
        println("[Hazard Detection] Hazard resolved by forwarding.")
        stall = false
        decodeStage()
      }
    } else {
      stall = false
      decodeStage()
    }
  }

  /**
   * Decode Stage
   */
  def decodeStage(): Unit = {
    // Decode logic implementation
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
  }

  /**
   * Execute Single Instruction
   */
  def execute(decoded: DecodedInstruction): (Int, Int, Boolean) = {
    if (decoded.isNop) {
      return (0, 0, false)
    }

    Metrics.totalInstructions += 1

    val op1 = decoded.op1
    val op2 = decoded.op2

    decoded.opcode match {
      case Alu if (decoded.isMul || decoded.isMulu) => executeMultiply(decoded, op1, op2)
      case Compress | Decompress => (0, 0, false) // Handled by compress stage
      case Ldt => executeLdt(decoded, op1)
      case Stt => executeStt(decoded, op1, op2)
      case Branch | BranchCf => executeBranch(decoded, op1)
      case _ => executeAlu(decoded, op1, op2)
    }
  }

  /**
   * Compress Stage Execution
   */
  def compressStage(): Unit = {
    if (compress_stage.validA && compress_stage.rdA != 0) {
      Metrics.compressInstructions += 1
      val compressedData = compressData(compress_stage.compressResultA)
      ex_wb = ex_wb.copy(
        memDataA = compressedData,
        rdA = compress_stage.rdA,
        validA = true
      )
    }

    if (compress_stage.validB && compress_stage.rdB != 0) {
      Metrics.decompressInstructions += 1
      val decompressedData = decompressData(compress_stage.compressResultB)
      ex_wb = ex_wb.copy(
        memDataB = decompressedData,
        rdB = compress_stage.rdB,
        validB = true
      )
    }

    compress_stage = COMPRESS_STAGE()
  }

  /**
   * Memory Stage
   */
  def memoryStage(): Unit = {
    // Memory access implementation
  }

  /**
   * Writeback Stage
   */
  def writebackStage(): Unit = {
    // Register writeback implementation
  }

  // Helper methods
  private def executeMultiply(decoded: DecodedInstruction, op1: Int, op2: Int): (Int, Int, Boolean) = {
    if (decoded.func != MUL && decoded.func != MULU) {
      println(s"[Execute] ALUm Error: Invalid function code ${decoded.func}")
      return (0, 0, false)
    }

    val product = if (decoded.isMulu) {
      (op1.toLong & 0xFFFFFFFFL) * (op2.toLong & 0xFFFFFFFFL)
    } else {
      op1.toLong * op2.toLong
    }

    val sl = (product & 0xFFFFFFFFL).toInt
    val sh = (product >>> 32).toInt

    if (decoded.rd < 31) {
      reg(decoded.rd) = sl
      reg(decoded.rd + 1) = sh
      if (decoded.isMulu) Metrics.muluInstructions += 1
      else Metrics.mulInstructions += 1
      (sl, sh, true)
    } else {
      println(s"[Execute] ALUm Error: rd=${decoded.rd} is the last register")
      (0, 0, false)
    }
  }

  private def executeLdt(decoded: DecodedInstruction, op1: Int): (Int, Int, Boolean) = {
    val address = op1 + (decoded.imm12 << 2)
    (address, decoded.rd, true)
  }

  private def executeStt(decoded: DecodedInstruction, op1: Int, op2: Int): (Int, Int, Boolean) = {
    val address = op1 + (decoded.imm12 << 2)
    (address, decoded.rd, true)
  }

  private def executeBranch(decoded: DecodedInstruction, op1: Int): (Int, Int, Boolean) = {
    if (op1 == 0) {
      flush = true
      Metrics.branchMispredictions += 1
      pc = pc + decoded.imm12
    }
    (0, 0, false)
  }

  private def executeAlu(decoded: DecodedInstruction, op1: Int, op2: Int): (Int, Int, Boolean) = {
    val aluResult = alu(decoded.func, op1, op2)
    if (decoded.rd != 0) {
      reg(decoded.rd) = aluResult
    }
    (aluResult, decoded.rd, true)
  }

  private def alu(func: Int, op1: Int, op2: Int): Int = {
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
      case _ => 0
    }
  }

  private def compressData(data: Int): Int = {
    // Simple compression: rotate left by 4 bits
    ((data << 4) | (data >>> 28)) & 0xFFFFFFFF
  }

  private def decompressData(data: Int): Int = {
    // Simple decompression: rotate right by 4 bits
    ((data >>> 4) | (data << 28)) & 0xFFFFFFFF
  }

  private def decodeInstruction(instr: Int): DecodedInstruction = {
    // Instruction decoding implementation
    DecodedInstruction()
  }

  private def checkForwardingCapability(decodedA: DecodedInstruction, decodedB: DecodedInstruction): Boolean = {
    // Forwarding logic implementation
    true
  }
}