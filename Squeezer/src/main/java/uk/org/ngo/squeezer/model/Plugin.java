/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.model;

import android.os.Parcel;

import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;


public class Plugin extends Item {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public Plugin setName(String name) {
        this.name = name;
        return this;
    }

    private String icon;

    /**
     * @return Relative URL path to an icon for this radio or music service, for example
     * "plugins/Picks/html/images/icon.png"
     */
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    private int iconResource;

    /**
     * @return Icon resource for this plugin if it is embedded in the Squeezer app, or null.
     */
    public int getIconResource() {
        return iconResource;
    }

    private int weight;

    public int getWeight() {
        return weight;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSearchable() {
        return (input != null);
    }


    public Plugin(Map<String, Object> record) {
        super(record);

        setId(getString(record, "cmd"));
        name = getString(record, "name");
        type = getString(record, "type");
        icon = getString(record, "icon");
        weight = getInt(record, "weight");

        if (name == null) name = getString(record, "text");
        if (icon == null) icon = getString(record, "icon-id");
        if (getId() == null) {
            setId(name.split("\n", 1)[0]);
        }
    }

    public static final Creator<Plugin> CREATOR = new Creator<Plugin>() {
        @Override
        public Plugin[] newArray(int size) {
            return new Plugin[size];
        }

        @Override
        public Plugin createFromParcel(Parcel source) {
            return new Plugin(source);
        }
    };

    private Plugin(Parcel source) {
        super(source);
        name = source.readString();
        type = source.readString();
        icon = source.readString();
        iconResource = source.readInt();
        weight = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(icon);
        dest.writeInt(iconResource);
        dest.writeInt(weight);
    }

    @Override
    public String toStringOpen() {
        return super.toStringOpen()
                + ", type: " + getType()
                + ", weight: " + getWeight()
                + ", go: " + goAction
                + ", play: " + playAction
                + ", add: " + addAction
                + ", insert: " + insertAction;
    }

    public boolean isPlayable() {
        return (playAction != null);
    }
}
