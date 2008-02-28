/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.mapred;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.*;

/** An {@link OutputFormat} that writes {@link SequenceFile}s. */
public class SequenceFileOutputFormat extends OutputFormatBase {

  public RecordWriter getRecordWriter(FileSystem ignored, JobConf job,
                                      String name, Progressable progress)
    throws IOException {

    Path outputPath = job.getOutputPath();
    FileSystem fs = outputPath.getFileSystem(job);
    if (!fs.exists(outputPath)) {
      throw new IOException("Output directory doesnt exist");
    }
    Path file = new Path(outputPath, name);
    CompressionCodec codec = null;
    CompressionType compressionType = CompressionType.NONE;
    if (getCompressOutput(job)) {
      // find the kind of compression to do
      compressionType = getOutputCompressionType(job);

      // find the right codec
      Class codecClass = getOutputCompressorClass(job, DefaultCodec.class);
      codec = (CompressionCodec) 
        ReflectionUtils.newInstance(codecClass, job);
    }
    final SequenceFile.Writer out = 
      SequenceFile.createWriter(fs, job, file,
                                job.getOutputKeyClass(),
                                job.getOutputValueClass(),
                                compressionType,
                                codec,
                                progress);

    return new RecordWriter<WritableComparable, Writable>() {

        public void write(WritableComparable key, Writable value)
          throws IOException {

          out.append(key, value);
        }

        public void close(Reporter reporter) throws IOException { out.close();}
      };
  }

  /** Open the output generated by this format. */
  public static SequenceFile.Reader[] getReaders(Configuration conf, Path dir)
    throws IOException {
    FileSystem fs = dir.getFileSystem(conf);
    Path[] names = fs.listPaths(dir);
    
    // sort names, so that hash partitioning works
    Arrays.sort(names);
    
    SequenceFile.Reader[] parts = new SequenceFile.Reader[names.length];
    for (int i = 0; i < names.length; i++) {
      parts[i] = new SequenceFile.Reader(fs, names[i], conf);
    }
    return parts;
  }

  /**
   * Get the {@link CompressionType} for the output {@link SequenceFile}.
   * @param conf the {@link JobConf}
   * @return the {@link CompressionType} for the output {@link SequenceFile}, 
   *         defaulting to {@link CompressionType#RECORD}
   */
  public static CompressionType getOutputCompressionType(JobConf conf) {
    String val = conf.get("mapred.output.compression.type", 
                          CompressionType.RECORD.toString());
    return CompressionType.valueOf(val);
  }
  
  /**
   * Set the {@link CompressionType} for the output {@link SequenceFile}.
   * @param conf the {@link JobConf} to modify
   * @param style the {@link CompressionType} for the output
   *              {@link SequenceFile} 
   */
  public static void setOutputCompressionType(JobConf conf, 
		                                          CompressionType style) {
    conf.set("mapred.output.compression.type", style.toString());
  }

}

