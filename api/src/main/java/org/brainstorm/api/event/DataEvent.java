package org.brainstorm.api.event;

import java.util.Objects;

import org.brainstorm.api.common.Header;

@SuppressWarnings("unused")
public class DataEvent {
    private Header header;
    private String name;
    private String address;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataEvent dataEvent = (DataEvent) o;
        return Objects.equals(header, dataEvent.header) && Objects.equals(name,
                dataEvent.name) && Objects.equals(address, dataEvent.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, name, address);
    }

    @Override
    public String toString() {
        return "DataEvent{" +
                "header=" + header +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
