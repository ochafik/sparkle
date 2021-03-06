/* Copyright 2013  Nest Labs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.  */
package nest.sparkle.loader.kafka

import scala.util.{Try, Success, Failure}

import _root_.kafka.serializer.Decoder
import nest.sparkle.loader._

/** extends ColumnDecoder with the ability to read streams of binary kafka data **/
sealed trait KafkaColumnDecoder[T] extends ColumnDecoder with Decoder[T] {
}

/** A KafkaColumnDecoder for records that contain a single id and multiple rows,
  * where each row contains a single key and multiple values.
  *
  * e.g. a record of the following structure:
  * id: "id",
  * row: [ [key,[value,..],
  *        [key,[value,..]
  *      ]
  */
trait KafkaKeyValues extends KeyValueColumn with KafkaColumnDecoder[ArrayRecordColumns] {
  /** report the types of the id, the key, and the values */
  override def metaData: ArrayRecordMeta
}

/** a KafkaColumnDecoder for id,[value] only records */
// trait KafkaValues // LATER

/** KafkaColumnDecoder for avro encoded key,value records */
case class KafkaKeyValueColumnDecoder[R]( serde: SparkleSerializer[R],
                                          decoder: ArrayRecordDecoder[R],
                                          override val suffix: String,
                                          override val prefix: String) extends KafkaKeyValues {

  /** Throws SparkleDeserializationException for deserialization errors and
    * ColumnDecoderException for decoding errors */
  override def fromBytes(bytes: Array[Byte]): ArrayRecordColumns = {
    Try(serde.fromBytes(bytes)) match {
      case Success(record) =>
        Try(decoder.decodeRecord(record)) match {
          case Success(decodedRecord) => decodedRecord
          case Failure(err)           => throw ColumnDecoderException(err)
        }
      case Failure(err)    =>
        throw SparkleDeserializationException(err)
    }
  }

  override def metaData: ArrayRecordMeta = decoder.metaData
}
