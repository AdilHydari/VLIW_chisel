//// File: PatSimApp.scala
//package patsim
//
//import java.io.FileInputStream
//
///**
// * Companion Object with Main Method
// */
//object PatSimApp {
//
//  def main(args: Array[String]): Unit = {
//    println("Simulating Patmos with Enhanced Pipeline and L1 Cache")
//    if (args.length != 1)
//      throw new Error("Wrong Arguments, usage: PatSimApp <binary_input_file>")
//    val instr = readBin(args(0))
//    val simulator = new PatSim(instr)
//    Metrics.reset()
//    println("Patmos start")
//
//    // Define a maximum cycle count to prevent infinite loops
//    val maxCycles: Int = 1000
//
//    while (!simulator.halt && simulator.pc < instr.length && Metrics.totalCycles < maxCycles) {
//      simulator.tick()
//    }
//
//    if (Metrics.totalCycles >= maxCycles) {
//      println("Maximum cycle count reached. Possible infinite loop detected.")
//    }
//
//    println("Simulation completed")
//    Metrics.printMetrics()      // Print performance metrics
//    simulator.l1Cache.printCacheMetrics() // Print detailed cache metrics
//  }
//
//  /**
//   * Read a binary file into an Array
//   * Corrected to read binary data and assemble 4-byte words into Ints
//   */
//  def readBin(fileName: String): Array[Int] = {
//
//    println("Reading " + fileName)
//    // Open the file as a binary input stream
//    val inputStream = new FileInputStream(fileName)
//    val byteArray = new Array[Byte](inputStream.available())
//    inputStream.read(byteArray)
//    inputStream.close()
//
//    // Convert bytes to Ints (4 bytes per Int, big-endian)
//    val numInts = byteArray.length / 4
//    val arr = new Array[Int](numInts)
//    for (i <- 0 until numInts) {
//      val byte0 = byteArray(i * 4) & 0xFF
//      val byte1 = byteArray(i * 4 + 1) & 0xFF
//      val byte2 = byteArray(i * 4 + 2) & 0xFF
//      val byte3 = byteArray(i * 4 + 3) & 0xFF
//      arr(i) = (byte0 << 24) | (byte1 << 16) | (byte2 << 8) | byte3
//    }
//
//    // Ensure at least one instruction
//    if (arr.isEmpty) {
//      Array(Constants.NOP)
//    } else {
//      arr
//    }
//  }
//}
