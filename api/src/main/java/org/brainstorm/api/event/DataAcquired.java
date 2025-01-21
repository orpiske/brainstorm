package org.brainstorm.api.event;

import java.util.Objects;

import org.brainstorm.api.common.Header;

@SuppressWarnings("unused")
public class DataAcquired extends DataEvent {
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DataAcquired that = (DataAcquired) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }

    @Override
    public String toString() {
        return "DataAcquired{" +
                "path='" + path + '\'' +
                "} " + super.toString();
    }
}
