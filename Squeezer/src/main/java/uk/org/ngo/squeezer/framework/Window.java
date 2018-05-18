/*
 * Copyright (c) 2017 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.framework;

import android.os.Parcel;

/**
 * Implements <window_fields> of the LMS SqueezePlay interface.
 * http://wiki.slimdevices.com/index.php/SqueezeCenterSqueezePlayInterface#.3Cwindow_fields.3E
 */
public class Window {
    public String text;
    public String textarea;
    public String textareaToken;
    public String iconId;
    public String icon;
    public String titleStyle;
    public String menuStyle;
    public HelpText help;
    public String windowId;

    public static Window readFromParcel(Parcel source) {
        if (source.readInt() == 0) return null;

        Window window = new Window();
        window.text = source.readString();
        window.textarea = source.readString();
        window.textareaToken = source.readString();
        window.iconId = source.readString();
        window.icon = source.readString();
        window.titleStyle = source.readString();
        window.menuStyle = source.readString();
        window.help = HelpText.readFromParcel(source);
        window.windowId = source.readString();

        return window;
    }

    public static void writeToParcel(Parcel dest, Window window) {
        dest.writeInt(window == null ? 0 : 1);
        if (window == null) return;

        dest.writeString(window.text);
        dest.writeString(window.textarea);
        dest.writeString(window.textareaToken);
        dest.writeString(window.iconId);
        dest.writeString(window.icon);
        dest.writeString(window.titleStyle);
        dest.writeString(window.menuStyle);
        HelpText.writeToParcel(dest, window.help);
        dest.writeString(window.windowId);
    }

    @Override
    public String toString() {
        return "Window{" +
                "text='" + text + '\'' +
                ", textarea='" + textarea + '\'' +
                ", textareaToken='" + textareaToken + '\'' +
                ", iconId='" + iconId + '\'' +
                ", icon='" + icon + '\'' +
                ", titleStyle='" + titleStyle + '\'' +
                ", menuStyle='" + menuStyle + '\'' +
                ", help=" + help +
                ", windowId='" + windowId + '\'' +
                '}';
    }

}