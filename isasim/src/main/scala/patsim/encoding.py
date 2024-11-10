#!/usr/bin/env python3
"""
Script: encode_instructions.py
Description: Automatically encodes COMPRESS and DECOMPRESS instructions into 32-bit hexadecimal format.
Usage:
    python3 encode_instructions.py compress <rd> <rs1> <rs2>
    python3 encode_instructions.py decompress <rd> <rs1> <rs2>
Example:
    python3 encode_instructions.py compress 5 6 7
    python3 encode_instructions.py decompress 8 9 10
"""

import sys

def encode_instruction(pred, opcode, rd, rs1, rs2, opc, func):
    """
    Encodes the instruction fields into a 32-bit integer.

    Parameters:
        pred (int): Predicate bits (0-31)
        opcode (int): Opcode (0-31)
        rd (int): Destination register (0-31)
        rs1 (int): Source register 1 (0-31)
        rs2 (int): Source register 2 (0-31)
        opc (int): Operation-specific bits (0-7)
        func (int): Function code (0-15)

    Returns:
        int: 32-bit encoded instruction
    """
    instruction = (pred << 27) | (opcode << 22) | (rd << 17) | (rs1 << 12) | (rs2 << 7) | (opc << 4) | func
    return instruction

def validate_register(reg):
    """
    Validates that the register number is between 0 and 31.

    Parameters:
        reg (str): Register number as string

    Returns:
        int: Register number as integer

    Raises:
        ValueError: If register number is out of range or invalid
    """
    try:
        reg_num = int(reg)
        if 0 <= reg_num <= 31:
            return reg_num
        else:
            raise ValueError(f"Register number {reg} out of range (0-31).")
    except ValueError:
        raise ValueError(f"Invalid register number: {reg}. Must be an integer between 0 and 31.")

def main():
    if len(sys.argv) != 5:
        print("Usage:")
        print("    python3 encode_instructions.py compress <rd> <rs1> <rs2>")
        print("    python3 encode_instructions.py decompress <rd> <rs1> <rs2>")
        print("Example:")
        print("    python3 encode_instructions.py compress 5 6 7")
        print("    python3 encode_instructions.py decompress 8 9 10")
        sys.exit(1)

    instr_type = sys.argv[1].lower()
    rd_str, rs1_str, rs2_str = sys.argv[2], sys.argv[3], sys.argv[4]

    # Validate instruction type
    if instr_type not in ['compress', 'decompress']:
        print(f"Invalid instruction type: {instr_type}. Must be 'compress' or 'decompress'.")
        sys.exit(1)

    # Validate and parse register numbers
    try:
        rd = validate_register(rd_str)
        rs1 = validate_register(rs1_str)
        rs2 = validate_register(rs2_str)
    except ValueError as ve:
        print(f"Register Error: {ve}")
        sys.exit(1)

    # Set opcode based on instruction type
    if instr_type == 'compress':
        opcode = 0x0c  # 12
    else:
        opcode = 0x0d  # 13

    # Set fixed fields
    pred = 0
    opc = 0
    func = 0

    # Encode instruction
    encoded_instr = encode_instruction(pred, opcode, rd, rs1, rs2, opc, func)

    # Format as 8-digit hexadecimal
    hex_instr = f"0x{encoded_instr:08X}"

    print(f"{instr_type.upper()} Instruction Encoded: {hex_instr}")

if __name__ == "__main__":
    main()
