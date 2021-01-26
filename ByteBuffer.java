package cn.logical.util;

import java.nio.charset.StandardCharsets;

/**
 * byte array util
 *
 * @author Jervis
 */
public class ByteBuffer {

    /**
     * 缓存数组
     */
    private byte[] cache;
    /**
     * 当前位置
     */
    private int position;
    /**
     * 标记
     */
    private int mark;
    /**
     * 写数据时该值与capacity 相等
     * 读数据时表示buffer 中有效数据长度
     */
    private int limit;
    /**
     * buffer 总容量
     */
    private int capacity;

    /**
     * 当前状态
     */
    private boolean read;

    private ByteBuffer() {
    }

    private ByteBuffer(int size) {
        setCache(new byte[size]);
        this.position = 0;
    }

    /**
     * 构建默认长度的 byte[]
     *
     * @return ByteArray
     */
    public static ByteBuffer allocate() {
        return new ByteBuffer(16);
    }

    /**
     * 构建 指定长度的 字节数组
     *
     * @param size 长度
     * @return ByteArray
     */
    public static ByteBuffer allocate(int size) {
        return new ByteBuffer(size);
    }

    public static ByteBuffer allocate (byte[] bytes) {
        ByteBuffer buffer = new ByteBuffer(bytes.length);
        buffer.put(bytes);
        return buffer;
    }

    public void put(byte[] bytes) {
        this.put(bytes, 0, bytes.length);
    }

    /**
     *
     * <p>
     * expandBuffer 检查缓存边界，判断是否能容纳传入的数组，并自动扩容
     *
     * @param bytes byte[]
     * @param off   byte[] index position
     * @param len   byte[] read length
     */
    public void put(byte[] bytes, int off, int len) {
        // 读 状态下修正状态为 写
        if (read) {
            position = limit;
            limit = capacity;
            mark = 0;
            read = false;
        }
        expandBuffer(bytes);
        System.arraycopy(bytes, off, cache, this.position, len);
        this.position += bytes.length;
    }

    public void put (byte data) {
        byte[] bytes = new byte[1];
        bytes[0] = data;
        put(bytes, 0, 1);
    }

    /**
     * 追加 int 字节数据
     *
     * @param data int 数据
     */
    public void put(int data) {
        this.put(DataConvert.getBytes(data), 0, 4);
    }

    /**
     * 追加 short 字节数据
     *
     * @param data short 数据
     */
    public void put(short data) {
        this.put(DataConvert.getBytes(data), 0, 2);
    }

    /**
     * 追加 long 字节数据
     *
     * @param data long 数据
     */
    public void put(long data) {
        this.put(DataConvert.getBytes(data), 0, 8);
    }

    /**
     * 追加 string 字节数据
     *
     * @param data string 数据
     */
    public void put(String data) {
        this.put(data.getBytes(StandardCharsets.UTF_8), 0, data.getBytes().length);
    }

    /**
     * 追加 int[] 字节数据
     * @param data int 数组
     */
    public void put(int[] data) {
        if (data.length <= 0) { return; }
        for (int i : data) {
            put(i);
        }
    }

    public byte readByte () {
        return read(1)[0];
    }

    /**
     * 读取2个长度的字节数据
     * 自动调整当前读取位
     *
     * @return 读取的 short 值
     */
    public short readShort() {
        return DataConvert.toShort(read(2));
    }

    /**
     * 读取4个长度的字节数据
     * 自动调整当前读取位
     *
     * @return 读取的 int 值
     */
    public int readInt() {
        return DataConvert.toInt(read(4));
    }

    /**
     * 读取8个长度的字节数据
     * 自动调整当前读取位
     *
     * @return 读取的 long 值
     */
    public long readLong() {
        return DataConvert.toLong(read(8));
    }

    /**
     * 读取对应长度的字节数据
     * 自动调整当前读取位
     *
     * @param size 读取长度
     * @return 读取的 string 值
     */
    public String readString(int size) {
        checkBounds(size);
        return DataConvert.toString(read(size));
    }

    /**
     * 以游标（position）为起始位读取对应长度的字节数组
     * 自动调整游标位置
     *
     * @param size 读取长度
     * @return byte array
     */
    private byte[] read(int size) {
        checkBounds(size);
        byte[] bytes = new byte[size];
        System.arraycopy(cache, position, bytes, 0, size);
        position += size;
        return bytes;
    }

    public byte[] readBytes(int size) {
        return read(size);
    }

    public byte[] getCache() {
        if (!read) {
            flip();
        }
        return DataConvert.copy(cache, position, limit);
    }

    private void setCache(byte[] cache) {
        this.cache = cache;
        this.limit = cache.length;
        this.capacity = cache.length;
    }

    /**
     * 检查 buffer 边界
     *
     * @param size 读取长度
     */
    private void checkBounds(int size) {
        if (size > limit || (position + size) > limit) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * 判断 缓存数组边界，自动扩充
     *
     * @param bytes byte[]
     */
    private void expandBuffer(byte[] bytes) {

        // 缓冲数组剩余容量 大于等于 传入数组长度则不扩充
        if (capacity - position >= bytes.length) {
            return;
        }

        // 对数组进行扩充
        byte[] temp = new byte[position + bytes.length];
        System.arraycopy(cache, 0, temp, 0, position);
        setCache(temp);

    }

    /**
     * buffer 转为可读
     */
    public void flip() {
        limit = position;
        position = 0;
        read = true;
    }

    public void clear() {
        position = 0;
    }

    /**
     * 标记当前读的位置
     */
    public void mark() {
        mark = position;
    }

    /**
     * 回到标记位
     * 令 buffer 可写
     */
    public void reset() {
        position = mark;
        read = false;
    }

    /**
     * 当前剩余可读长度
     *
     * @return int
     */
    public int canReadSize() {
        return read ? limit - position : -1;
    }

    /**
     * 修正缓存序列
     * 去除已读部分
     * 修正序列总容量、有效边界、游标、标记位
     */
    public void revise() {
        if (read && position > 0) {
            byte[] bytes = new byte[capacity - position];
            if (capacity - position > 0) {
                System.arraycopy(cache, position, bytes, 0, capacity - position);
            }
            cache = bytes;
            capacity = limit = bytes.length;
            position = 0;
            mark = 0;
            read = false;
        }
    }

    /**
     * copy cache 有效长度
     * @param size 长度
     * @return cache 有效内容
     */
    private byte[] copy (int size) {
        checkBounds(size);
        byte[] bytes = new byte[size];
        System.arraycopy(cache, 0, bytes, 0, size);
        return bytes;
    }

    public void clean () {
        setCache(new byte[16]);
        this.position = 0;
        this.mark = 0;
        read = false;
    }

    public int getPosition() {
        return position;
    }

    public int getLimit() {
        return limit;
    }

}
