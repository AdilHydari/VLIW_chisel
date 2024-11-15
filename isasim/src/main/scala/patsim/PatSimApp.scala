// File: PatSimApp.scala
package patsim

import java.io.{File, FileInputStream}
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object PatSimApp {
  def main(args: Array[String]): Unit = {
    println("VLIW Processor Simulator with Enhanced Pipeline and L1 Cache")
    
    if (args.length != 1) {
      println("Usage: PatSimApp <binary_input_file>")
      println("Example: PatSimApp program.bin")
      System.exit(1)
    }

    // Validate input file
    val inputFile = new File(args(0))
    if (!inputFile.exists() || !inputFile.isFile) {
      println(s"Error: Input file '${args(0)}' does not exist or is not a regular file")
      System.exit(1)
    }

    try {
      val instructions = readBin(args(0))
      println(s"Loaded ${instructions.length} instructions")
      
      // Create simulator components
      val simulator = new PatSim(instructions)
      val branchUnit = new BranchUnit()
      val compressionUnit = new CompressionUnit()
      val bundler = new InstructionBundle()
      
      // Reset all metrics
      Metrics.reset()
      simulator.l1Cache.reset()
      compressionUnit.reset()
      
      println("\nStarting simulation...")
      
      // Create a promise for simulation completion
      val simulationComplete = Promise[Unit]()
      
      // Run simulation in background
      Future {
        val maxCycles = 10000 // Increased cycle limit
        var cycleCount = 0
        
        while (!simulator.halt && simulator.pc < instructions.length && cycleCount < maxCycles) {
          // Execute one cycle
          simulator.tick()
          cycleCount += 1
          Metrics.incrementCycles()
          
          // Print progress every 1000 cycles
          if (cycleCount % 1000 == 0) {
            println(s"Executed $cycleCount cycles...")
          }
          
          // Small delay to prevent CPU overload
          Thread.sleep(0, 100000) // 100 microseconds
        }
        
        // Check termination condition
        val terminationReason = 
          if (simulator.halt) "Halt instruction encountered"
          else if (simulator.pc >= instructions.length) "End of program reached"
          else "Maximum cycle count exceeded"
        
        simulationComplete.success(())
        println(s"\nSimulation ended: $terminationReason")
      }
      
      // Handle simulation completion
      simulationComplete.future.onComplete {
        case Success(_) =>
          println("\nSimulation Statistics:")
          println("--------------------")
          Metrics.printMetrics()
          simulator.l1Cache.printCacheMetrics()
          
          // Print compression statistics if compression instructions were used
          if (Metrics.compressInstructions > 0 || Metrics.decompressInstructions > 0) {
            println("\nCompression Unit Statistics:")
            println(s"Dictionary Size: ${compressionUnit.getDictionarySize}")
            println(f"Compression Ratio: ${compressionUnit.getCompressionRatio * 100}%.2f%%")
          }
          
          println("\nSimulation completed successfully")
          
        case Failure(ex) =>
          println(s"\nSimulation failed with error: ${ex.getMessage}")
          ex.printStackTrace()
      }
      
      // Wait for simulation to complete
      scala.concurrent.Await.result(simulationComplete.future, scala.concurrent.duration.Duration.Inf)
      
    } catch {
      case ex: Exception =>
        println(s"Error during simulation: ${ex.getMessage}")
        ex.printStackTrace()
        System.exit(1)
    }
  }

  def readBin(fileName: String): Array[Int] = {
    println(s"Reading binary file: $fileName")
    var inputStream: FileInputStream = null
    
    try {
      inputStream = new FileInputStream(fileName)
      val fileSize = inputStream.available()
      
      if (fileSize % 4 != 0) {
        throw new IllegalArgumentException(
          s"Invalid binary file size: $fileSize bytes (must be multiple of 4)")
      }
      
      val byteArray = new Array[Byte](fileSize)
      val bytesRead = inputStream.read(byteArray)
      
      if (bytesRead != fileSize) {
        throw new IllegalStateException(
          s"Failed to read entire file: read $bytesRead of $fileSize bytes")
      }
      
      // Convert bytes to instructions (4 bytes per instruction, big-endian)
      val numInstructions = fileSize / 4
      val instructions = new Array[Int](numInstructions)
      
      for (i <- 0 until numInstructions) {
        val offset = i * 4
        instructions(i) = ((byteArray(offset) & 0xFF) << 24) |
                         ((byteArray(offset + 1) & 0xFF) << 16) |
                         ((byteArray(offset + 2) & 0xFF) << 8) |
                         (byteArray(offset + 3) & 0xFF)
      }
      
      // Ensure at least one instruction
      if (instructions.isEmpty) {
        println("Warning: Empty input file, using NOP instruction")
        Array(Constants.NOP)
      } else {
        instructions
      }
      
    } catch {
      case ex: Exception =>
        println(s"Error reading binary file: ${ex.getMessage}")
        throw ex
        
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close()
        } catch {
          case _: Exception => // Ignore close errors
        }
      }
    }
  }
}
