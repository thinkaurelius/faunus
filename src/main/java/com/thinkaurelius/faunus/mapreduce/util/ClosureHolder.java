package com.thinkaurelius.faunus.mapreduce.util;

import groovy.lang.Closure;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ClosureHolder<T> implements Serializable {

    private Closure<T> closure;

    public ClosureHolder(final Closure<T> closure) {
        this.closure = closure;
    }

    public Closure<T> getClosure() {
        return this.closure;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.writeObject(this.closure.dehydrate());
    }

    private void readObject(final ObjectInputStream input) throws IOException, ClassNotFoundException {
        final Closure<T> c = (Closure<T>) input.readObject();
        this.closure = c.rehydrate(this, this, this);
    }

    public String serialize() {
        try {
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(stream);
            this.writeObject(outputStream);
            outputStream.flush();
            return new BASE64Encoder().encode(stream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    public static <T> ClosureHolder<T> deserialize(final String bytes) throws IOException, InterruptedException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(bytes)));
        try {
            ClosureHolder<T> holder = new ClosureHolder<T>(null);
            holder.readObject(inputStream);
            return holder;
        } catch (ClassNotFoundException e) {
            throw new InterruptedException(e.getMessage());
        }
    }
}
