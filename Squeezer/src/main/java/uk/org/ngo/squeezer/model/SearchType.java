/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

import com.mikepenz.iconics.typeface.IIcon;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;


public class SearchType<T extends BaseItemView> {

    private String mTitle;
    private IIcon mIconResourse;
    private T mViewBuilder;
    private String mModelClassName;
    private boolean mExpand = false;
    private String pluginId;
    private String parentPluginId;
    private boolean custom = false;

    public SearchType(){

    }

    public SearchType(String t, IIcon iconr, T view, String m) {
        mTitle = t;
        mIconResourse = iconr;
        mViewBuilder = view;
        mModelClassName = m;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public IIcon getIconResourse() {
        return mIconResourse;
    }

    public void setIconResourse(IIcon mIconResourse) {
        this.mIconResourse = mIconResourse;
    }

    public T getViewBuilder() {
        return (T) mViewBuilder;
    }

    public void setViewBuilder(T mViewBuilder) {
        this.mViewBuilder = mViewBuilder;
    }

    public String getModelClassName() {
        return mModelClassName;
    }

    public void setModelClassName(String mModelClassName) {
        this.mModelClassName = mModelClassName;
    }

    public boolean isExpand() {
        return mExpand;
    }

    public void setExpand(boolean mExpand) {
        this.mExpand = mExpand;
    }

    public void toggleExpand() {
        if (this.mExpand) {
            this.mExpand = false;
        }else{
            this.mExpand = true;
        }
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public void setParentPluginId(String parentPluginId) {
        this.parentPluginId = parentPluginId;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean getCustom() {
        return custom;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getParentPluginId() {
        return parentPluginId;
    }
}
