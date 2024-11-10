//// File: L1Cache.scala
//package patsim
//
//class L1Cache(cacheSize: Int = 16) { // 16 lines for simplicity
//  // Each cache line holds a tag and data
//  case class CacheLine(var tag: Int, var data: Int)
//
//  // Initialize cache with invalid tags
//  private val cache = Array.fill(cacheSize)(CacheLine(-1, 0))
//
//  // Additional Metrics
//  var readHits: Int = 0
//  var readMisses: Int = 0
//  var writeHits: Int = 0
//  var writeMisses: Int = 0
//
//  /**
//   * Access the cache for a given address.
//   * Returns (hit: Boolean, data: Int)
//   */
//  def access(address: Int, isLoad: Boolean, writeData: Int = 0): (Boolean, Int) = {
//    val index = (address / 4) % cacheSize
//    val tag = address / (4 * cacheSize)
//    val line = cache(index)
//
//    if (line.tag == tag) {
//      if (isLoad) {
//        readHits += 1
//        Metrics.cacheHits += 1 // Increment total cache hits
//        // Cache Hit: Return data
//        println(s"[L1Cache] Load Hit at index $index for address $address")
//        (true, line.data)
//      } else {
//        writeHits += 1
//        Metrics.cacheHits += 1 // Increment total cache hits
//        // Store Hit: Update data
//        line.data = writeData
//        println(s"[L1Cache] Store Hit at index $index for address $address with data $writeData")
//        (true, 0)
//      }
//    } else {
//      if (isLoad) {
//        readMisses += 1
//        Metrics.cacheMisses += 1 // Increment total cache misses
//        // Cache Miss: For simulation, assume data is 0
//        println(s"[L1Cache] Load Miss at index $index for address $address. Loading data=0")
//        cache(index) = CacheLine(tag, 0) // Load data from memory (simulated as 0)
//        (false, 0)
//      } else {
//        writeMisses += 1
//        Metrics.cacheMisses += 1 // Increment total cache misses
//        // Cache Miss: For store, write directly to memory (simulated)
//        println(s"[L1Cache] Store Miss at index $index for address $address. Writing data=$writeData to memory")
//        (false, 0)
//      }
//    }
//  }
//
//  /**
//   * Print Detailed Cache Metrics
//   */
//  def printCacheMetrics(): Unit = {
//    println("\n=== L1 Cache Detailed Metrics ===")
//    println(s"Read Hits: $readHits")
//    println(s"Read Misses: $readMisses")
//    println(s"Write Hits: $writeHits")
//    println(s"Write Misses: $writeMisses")
//    println(f"Read Hit Rate: ${if (readHits + readMisses > 0) (readHits.toDouble / (readHits + readMisses)) * 100 else 0}%.2f%%")
//    println(f"Write Hit Rate: ${if (writeHits + writeMisses > 0) (writeHits.toDouble / (writeHits + writeMisses)) * 100 else 0}%.2f%%")
//    println("==============================\n")
//  }
//}
