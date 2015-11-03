package com.xhr.FileUpDown;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.xhr.FileUpDown.download.DownloadInfo;

/**
 * Contains the configuration of the upload notification.
 *
 * @author iflytek (Alex Gotev)
 */
public class NotificationConfig implements Parcelable {


    private int id;
    private final int iconResourceID;
    private final String title;
    private final String message;
    private final String completed;
    private final String error;
    private final String cancel;
    private final String pause;
    private final boolean autoClearOnSuccess;
    private boolean ringTone;
    private Intent clickIntent;

    public NotificationConfig() {
        id = 0;
        iconResourceID = android.R.drawable.ic_menu_upload;
        title = "FileUpDown";
        message = "task in progress";
        completed = "task completed successfully!";
        error = "error during task process";
        cancel = "task is canceled";
        pause="task is pause";
        autoClearOnSuccess = false;
        clickIntent = null;
        ringTone = false;
    }

    public NotificationConfig(final DownloadInfo downloadInfo, final int iconResourceID, final String title,
                              final String message, final String completed,
                              final String error, final String cancel, final String pause,final boolean autoClearOnSuccess)
            throws IllegalArgumentException {

        if (title == null || message == null || completed == null || error == null) {
            throw new IllegalArgumentException("You can't provide null parameters");
        }
        this.id =  downloadInfo.getId().hashCode();
        this.iconResourceID = iconResourceID;
        this.title = title;
        this.message = message;
        this.completed = completed;
        this.error = error;
        this.cancel = cancel;
        this.pause=pause;
        this.autoClearOnSuccess = autoClearOnSuccess;
    }

    public final int getId() {
        return id;
    }

    public final int getIconResourceID() {
        return iconResourceID;
    }

    public final String getTitle() {
        return title;
    }

    public final String getMessage() {
        return message;
    }

    public final String getCompleted() {
        return completed;
    }

    public final String getError() {
        return error;
    }

    public final String getCancel() {
        return cancel;
    }

    public final String getPause(){return  pause;}

    public final boolean isAutoClearOnSuccess() {
        return autoClearOnSuccess;
    }

    public final boolean isRingTone() {
        return ringTone;
    }

    public final PendingIntent getPendingIntent(Context context) {
        if (clickIntent == null) {
            return PendingIntent.getBroadcast(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return PendingIntent.getActivity(context, 1, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public final void setClickIntent(Intent clickIntent) {
        this.clickIntent = clickIntent;
    }

    public final void enableRingTone(Boolean tone) {
        this.ringTone = tone;
    }


    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<NotificationConfig> CREATOR =
            new Creator<NotificationConfig>() {
                @Override
                public NotificationConfig createFromParcel(final Parcel in) {
                    return new NotificationConfig(in);
                }

                @Override
                public NotificationConfig[] newArray(final int size) {
                    return new NotificationConfig[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(id);
        parcel.writeInt(iconResourceID);
        parcel.writeString(title);
        parcel.writeString(message);
        parcel.writeString(completed);
        parcel.writeString(error);
        parcel.writeString(cancel);
        parcel.writeString(pause);
        parcel.writeByte((byte) (autoClearOnSuccess ? 1 : 0));
        parcel.writeByte((byte) (ringTone ? 1 : 0));
        parcel.writeParcelable(clickIntent, 0);
    }

    private NotificationConfig(Parcel in) {
        id=in.readInt();
        iconResourceID = in.readInt();
        title = in.readString();
        message = in.readString();
        completed = in.readString();
        error = in.readString();
        cancel = in.readString();
        pause=in.readString();
        autoClearOnSuccess = in.readByte() == 1;
        ringTone = in.readByte() == 1;
        clickIntent = in.readParcelable(Intent.class.getClassLoader());
    }
}
