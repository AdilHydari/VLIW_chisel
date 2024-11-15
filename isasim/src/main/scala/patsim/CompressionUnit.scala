package patsim

import scala.collection.mutable

class CompressionUnit {
  private val dictionary = new mutable.HashMap[Int, Short]()
  private var nextCode: Short = 0

  def compress(data: Int): Int = synchronized {
    if (dictionary.contains(data)) {
      Metrics.compressionHits += 1
      dictionary(data).toInt
    } else {
      if (nextCode < Short.MaxValue) {
        dictionary(data) = nextCode
        nextCode = (nextCode + 1).toShort
        Metrics.compressionMisses += 1
        data
      } else {
        // Dictionary full, return original data
        Metrics.compressionMisses += 1
        data
      }
    }
  }

  def decompress(data: Int): Int = synchronized {
    if (data <= Short.MaxValue) {
      dictionary.find(_._2 == data.toShort) match {
        case Some((original, _)) =>
          Metrics.decompressionHits += 1
          original
        case None =>
          Metrics.decompressionMisses += 1
          data
      }
    } else {
      // Data wasn't compressed
      data
    }
  }

  def reset(): Unit = synchronized {
    dictionary.clear()
    nextCode = 0
  }

  def getDictionarySize: Int = dictionary.size
  def getCompressionRatio: Double = {
    val totalEntries = dictionary.size
    if (totalEntries > 0) {
      val originalSize = totalEntries * 4 // 4 bytes per Int
      val compressedSize = totalEntries * 2 // 2 bytes per Short
      compressedSize.toDouble / originalSize
    } else {
      1.0
    }
  }
}