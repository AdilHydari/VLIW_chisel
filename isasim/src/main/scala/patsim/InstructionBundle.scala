package patsim

import patsim.Opcode._

class InstructionBundle {
  def canBundle(instr1: DecodedInstruction, instr2: DecodedInstruction): Boolean = {
    // Don't bundle if either instruction is NOP
    if (instr1.isNop || instr2.isNop) return false

    // Check register dependencies
    if (hasRegisterDependency(instr1, instr2)) return false

    // Check instruction type compatibility
    if (!areInstructionTypesCompatible(instr1, instr2)) return false

    true
  }

  private def hasRegisterDependency(instr1: DecodedInstruction, instr2: DecodedInstruction): Boolean = {
    // Check write-after-write (WAW) hazard
    if (instr1.rd != 0 && instr1.rd == instr2.rd) return true

    // Check write-after-read (WAR) hazard
    if (instr1.rd != 0 && (instr1.rd == instr2.rs1 || instr1.rd == instr2.rs2)) return true

    // Check read-after-write (RAW) hazard
    if (instr2.rd != 0 && (instr2.rd == instr1.rs1 || instr2.rd == instr1.rs2)) return true

    false
  }

  private def areInstructionTypesCompatible(instr1: DecodedInstruction, instr2: DecodedInstruction): Boolean = {
    // Define incompatible instruction pairs
    val incompatiblePairs = Set(
      (Branch, Branch),
      (BranchCf, Branch),
      (Branch, BranchCf),
      (BranchCf, BranchCf),
      (Compress, Compress),
      (Decompress, Decompress),
      (Compress, Decompress),
      (Decompress, Compress)
    )

    // Check if the instruction pair is incompatible
    if (incompatiblePairs.contains((instr1.opcode, instr2.opcode))) return false

    // Special handling for multiply instructions
    if (instr1.isMul || instr1.isMulu) {
      if (instr2.isMul || instr2.isMulu) return false
      // Check if instr2 tries to use the high word register
      if (instr2.rs1 == instr1.rd + 1 || instr2.rs2 == instr1.rd + 1) return false
    }

    true
  }

  def bundle(pc: Int, instructions: Array[Int]): (DecodedInstruction, DecodedInstruction) = {
    if (pc >= instructions.length) {
      return (DecodedInstruction(), DecodedInstruction())
    }

    val instr1 = decodeInstruction(instructions(pc))
    
    if (pc + 1 >= instructions.length) {
      return (instr1, DecodedInstruction())
    }

    val instr2 = decodeInstruction(instructions(pc + 1))
    
    if (canBundle(instr1, instr2)) {
      (instr1, instr2)
    } else {
      (instr1, DecodedInstruction())
    }
  }

  private def decodeInstruction(instr: Int): DecodedInstruction = {
    val opcode = (instr >>> 26) & 0x3F
    val rd = (instr >>> 21) & 0x1F
    val rs1 = (instr >>> 16) & 0x1F
    val rs2 = (instr >>> 11) & 0x1F
    val func = instr & 0x7FF
    val imm12 = instr & 0xFFF
    val opc = (instr >>> 6) & 0x1F

    DecodedInstruction(
      opcode = opcode,
      rd = rd,
      rs1 = rs1,
      rs2 = rs2,
      func = func,
      imm12 = imm12,
      opc = opc,
      isNop = (instr == 0),
      isMul = (opcode == Alu && func == Function.MUL),
      isMulu = (opcode == Alu && func == Function.MULU),
      isCompress = (opcode == Compress),
      isDecompress = (opcode == Decompress)
    )
  }
}