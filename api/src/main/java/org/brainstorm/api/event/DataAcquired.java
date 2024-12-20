package org.brainstorm.api.event;

import org.brainstorm.api.common.Header;

@SuppressWarnings("unused")
public class DataAcquired {
    private Header header;
    private String name;
    private String address;
    private String path;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "DataAcquired{" +
                "header=" + header +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
