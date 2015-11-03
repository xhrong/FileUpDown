package com.xhr.FileUpDown.upload;

import android.os.Parcel;
import android.os.Parcelable;
import com.xhr.FileUpDown.upload.utils.ContentType;

import java.io.*;

/**
 * An HTTP Multipart file to upload.
 *
 * @author iflytek (Alex Gotev)
 * @author eliasnaur
 *
 */
class UploadFile  implements Parcelable {

    protected final File file;

    private static final String NEW_LINE = "\r\n";

    protected final String paramName;
    protected final String fileName;
    protected String contentType;

    /**
     * Create a new UploadFile object.
     *
     * @param path absolute path to the file
     * @param parameterName parameter name to use in the multipart form
     * @param contentType content type of the file to send
     */
    public UploadFile(final String path, final String parameterName,
                      final String fileName, final String contentType) {

        this.file = new File(path);

        this.paramName = parameterName;
        this.contentType = contentType;

        if (fileName == null || "".equals(fileName)) {
            this.fileName = this.file.getName();
        } else {
            this.fileName = fileName;
        }
    }

    public long length() {
        return file.length();
    }

    public final InputStream getStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }


    public byte[] getMultipartHeader() throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();

        builder.append("Content-Disposition: form-data; name=\"")
               .append(paramName).append("\"; filename=\"")
               .append(fileName).append("\"").append(NEW_LINE);

        if (contentType == null) {
            contentType = ContentType.APPLICATION_OCTET_STREAM;
        }

        builder.append("Content-Type: ").append(contentType).append(NEW_LINE).append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    /**
     * Get the total number of bytes needed by this file in the HTTP/Multipart request, considering
     * that to send each file there is some overhead due to some bytes needed for the boundary
     * and some bytes needed for the multipart headers
     *
     * @param boundaryBytesLength length in bytes of the multipart boundary
     * @return total number of bytes needed by this file in the HTTP/Multipart request
     * @throws java.io.UnsupportedEncodingException
     */
    public long getTotalMultipartBytes(long boundaryBytesLength) throws UnsupportedEncodingException {
        return boundaryBytesLength + getMultipartHeader().length + file.length();
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<UploadFile> CREATOR =
            new Creator<UploadFile>() {
        @Override
        public UploadFile createFromParcel(final Parcel in) {
            return new UploadFile(in);
        }

        @Override
        public UploadFile[] newArray(final int size) {
            return new UploadFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(file.getAbsolutePath());
        parcel.writeString(paramName);
        parcel.writeString(contentType);
        parcel.writeString(fileName);
    }


    private UploadFile(Parcel in) {
        file = new File(in.readString());
        paramName = in.readString();
        contentType = in.readString();
        fileName = in.readString();
    }
}
