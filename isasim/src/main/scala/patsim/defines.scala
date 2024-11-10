// File: defines.scala
package patsim

/**
 * Opcode Definitions
 * Each opcode is represented as an integer corresponding to its binary encoding.
 */
object Opcode {
  val AluImm     = 0x00        // Opcode for ALU Immediate Instructions
  val Alu        = 0x08        // Opcode for ALUm (Multiply) Instruction
  val AluLongImm = 0x1F        // Opcode for ALU Long Immediate Instructions
  val Branch     = 0x13        // Opcode for Branch Instructions
  val BranchCf   = 0x15        // Opcode for Conditional Branch Instructions
  val Ldt        = 0x0A        // Opcode for Load Typed Instructions
  val Stt        = 0x0B        // Opcode for Store Typed Instructions
  val Compress   = 0x0C        // Opcode for COMPRESS Instruction
  val Decompress = 0x0D        // Opcode for DECOMPRESS Instruction
  // Note: DIV Opcode has been omitted as per requirements
}

/**
 * Function Codes for ALU Operations
 * Each function code is represented as an integer corresponding to its binary encoding.
 */
object Function {
  val ADD      = 0x0          // Function code for ADD
  val SUB      = 0x1          // Function code for SUB
  val XOR      = 0x2          // Function code for XOR
  val SL       = 0x3          // Function code for Shift Left
  val SR       = 0x4          // Function code for Shift Right
  val SRA      = 0x5          // Function code for Shift Right Arithmetic
  val OR       = 0x6          // Function code for OR
  val AND      = 0x7          // Function code for AND
  val NOR      = 0xB          // Function code for NOR
  val SHADD    = 0xC          // Function code for SHADD
  val SHADD2   = 0xD          // Function code for SHADD2

  // Function Codes for ALUm (Multiply) Instruction
  val MUL      = 0x0          // Function code for MUL (Signed Multiply)
  val MULU     = 0x1          // Function code for MULU (Unsigned Multiply)

  // Note: DIV Function Codes have been omitted as per requirements
}

/**
 * Constants Used Across the Simulator
 */
object Constants {
  val NOP = 0x00000000        // No Operation Instruction

  // Additional constants can be defined here as needed
}
