package com.thinkaurelius.faunus.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.TaskAttemptID;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.task.MapContextImpl;
import org.apache.hadoop.security.Credentials;

/**
 * MemoryMapper supports in-memory mapping for a chain of consecutive mappers.
 * This provides significant performance improvements as each map need not write
 * its results to disk. Note that MemoryMapper is not general-purpose and is
 * specific to Faunus' current MapReduce library. In particular, it assumes that
 * the chain of mappers emits 0 or 1 key/value pairs for each input.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class MemoryMapper<A, B, C, D> extends Mapper<A, B, C, D> {

    public class MemoryMapContext extends Mapper.Context{

        private MapContextImpl<A,B,C,D> mapContextImpl;

        private static final String DASH = "-";
        private static final String EMPTY = "";
        private final Configuration currentConfiguration = new Configuration();
        private Writable key = null;
        private Writable value = null;
        private Writable tempKey = null;
        private Writable tempValue = null;
        private Mapper.Context context;
        private Configuration globalConfiguration;

        public MemoryMapContext(final Mapper.Context context) throws IOException, InterruptedException {

            mapContextImpl = new MapContextImpl<A, B, C, D>(context.getConfiguration(), context.getTaskAttemptID() == null ? new TaskAttemptID() : context.getTaskAttemptID(), null, null, context.getOutputCommitter(), null, context.getInputSplit());

            this.context = context;
            this.globalConfiguration = context.getConfiguration();
        }

        @Override
        public void write(final Object key, final Object value) throws IOException, InterruptedException {
            this.key = (Writable) key;
            this.value = (Writable) value;
        }

        @Override
        public Writable getCurrentKey() {
            this.tempKey = this.key;
            this.key = null;
            return this.tempKey;
        }

        @Override
        public Writable getCurrentValue() {
            this.tempValue = this.value;
            this.value = null;
            return tempValue;
        }

        @Override
        public boolean nextKeyValue() {
            return this.key != null && this.value != null;
        }

        @Override
        public Counter getCounter(final String groupName, final String counterName) {
            return this.context.getCounter(groupName, counterName);
        }

        @Override
        public Counter getCounter(final Enum counterName) {
            return this.context.getCounter(counterName);
        }

        @Override
        public Configuration getConfiguration() {
            return this.currentConfiguration;
        }

        public void setContext(final Mapper.Context context) {
            this.context = context;
        }

        public void stageConfiguration(final int step) {
            this.currentConfiguration.clear();
            for (final Map.Entry<String, String> entry : this.globalConfiguration) {
                final String key = entry.getKey();
                if (key.endsWith(DASH + step)) {
                    this.currentConfiguration.set(key.replace(DASH + step, EMPTY), entry.getValue());
                } else if (!key.matches(".*-[0-9]+")) {
                    this.currentConfiguration.set(key, entry.getValue());
                }
            }
        }

        @Override
        public InputSplit getInputSplit() {
            return mapContextImpl.getInputSplit();

        }

        @Override
        public OutputCommitter getOutputCommitter() {
            return mapContextImpl.getOutputCommitter();
        }

        @Override
        public TaskAttemptID getTaskAttemptID() {
            return mapContextImpl.getTaskAttemptID();
        }

        @Override
        public void setStatus(String msg) {
            mapContextImpl.setStatus(msg);
        }

        @Override
        public String getStatus() {
            return mapContextImpl.getStatus();
        }

        @Override
        public float getProgress() {
            return mapContextImpl.getProgress();
        }

        @Override
        public Credentials getCredentials() {
            return mapContextImpl.getCredentials();
        }

        @Override
        public JobID getJobID() {
            return mapContextImpl.getJobID();
        }

        @Override
        public int getNumReduceTasks() {
            return mapContextImpl.getNumReduceTasks();
        }

        @Override
        public Path getWorkingDirectory() throws IOException {
            return mapContextImpl.getWorkingDirectory();
        }

        @Override
        public Class<?> getOutputKeyClass() {
            return mapContextImpl.getOutputKeyClass();
        }

        @Override
        public Class<?> getOutputValueClass() {
            return mapContextImpl.getOutputValueClass();
        }

        @Override
        public Class<?> getMapOutputKeyClass() {
            return mapContextImpl.getMapOutputKeyClass();
        }

        @Override
        public Class<?> getMapOutputValueClass() {
            return mapContextImpl.getMapOutputValueClass();
        }

        @Override
        public String getJobName() {
            return mapContextImpl.getJobName();
        }

        @Override
        public Class<? extends InputFormat<?, ?>> getInputFormatClass() throws ClassNotFoundException {
            return mapContextImpl.getInputFormatClass();
        }

        @Override
        public Class<? extends Mapper<?, ?, ?, ?>> getMapperClass() throws ClassNotFoundException {
            return mapContextImpl.getMapperClass();
        }

        @Override
        public Class<? extends Reducer<?, ?, ?, ?>> getCombinerClass() throws ClassNotFoundException {
            return mapContextImpl.getCombinerClass();
        }

        @Override
        public Class<? extends Reducer<?, ?, ?, ?>> getReducerClass() throws ClassNotFoundException {
            return mapContextImpl.getReducerClass();
        }

        @Override
        public Class<? extends OutputFormat<?, ?>> getOutputFormatClass() throws ClassNotFoundException {
            return mapContextImpl.getOutputFormatClass();
        }

        @Override
        public Class<? extends Partitioner<?, ?>> getPartitionerClass() throws ClassNotFoundException {
            return mapContextImpl.getPartitionerClass();
        }

        @Override
        public RawComparator<?> getSortComparator() {
            return mapContextImpl.getSortComparator();
        }

        @Override
        public String getJar() {
            return mapContextImpl.getJar();
        }

        @Override
        public RawComparator<?> getGroupingComparator() {
            return mapContextImpl.getGroupingComparator();
        }

        @Override
        public boolean getJobSetupCleanupNeeded() {
            return mapContextImpl.getJobSetupCleanupNeeded();
        }

        @Override
        public boolean getTaskCleanupNeeded() {
            return mapContextImpl.getTaskCleanupNeeded();
        }

        @Override
        public boolean getProfileEnabled() {
            return mapContextImpl.getProfileEnabled();
        }

        @Override
        public String getProfileParams() {
            return mapContextImpl.getProfileParams();
        }

        @Override
        public Configuration.IntegerRanges getProfileTaskRange(boolean isMap) {
            return mapContextImpl.getProfileTaskRange(isMap);
        }

        @Override
        public String getUser() {
            return mapContextImpl.getUser();
        }

        @Override
        public boolean getSymlink() {
            return mapContextImpl.getSymlink();
        }

        @Override
        public Path[] getArchiveClassPaths() {
            return mapContextImpl.getArchiveClassPaths();
        }

        @Override
        public URI[] getCacheArchives() throws IOException {
            return mapContextImpl.getCacheArchives();
        }

        @Override
        public URI[] getCacheFiles() throws IOException {
            return mapContextImpl.getCacheFiles();
        }

        @Override
        public Path[] getLocalCacheArchives() throws IOException {
            return mapContextImpl.getLocalCacheArchives();
        }

        @Override
        public Path[] getLocalCacheFiles() throws IOException {
            return mapContextImpl.getLocalCacheFiles();
        }

        @Override
        public Path[] getFileClassPaths() {
            return mapContextImpl.getFileClassPaths();
        }

        @Override
        public String[] getArchiveTimestamps() {
            return mapContextImpl.getArchiveTimestamps();
        }

        @Override
        public String[] getFileTimestamps() {
            return mapContextImpl.getFileTimestamps();
        }

        @Override
        public int getMaxMapAttempts() {
            return mapContextImpl.getMaxMapAttempts();
        }

        @Override
        public int getMaxReduceAttempts() {
            return mapContextImpl.getMaxReduceAttempts();
        }

        @Override
        public void progress() {
            mapContextImpl.progress();
        }


    }
}