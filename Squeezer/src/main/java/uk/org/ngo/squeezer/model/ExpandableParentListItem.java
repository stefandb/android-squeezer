package uk.org.ngo.squeezer.model;


import com.bignerdranch.expandablerecyclerview.Model.ParentObject;
import com.mikepenz.iconics.typeface.IIcon;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Stefan on 6-1-2016.
 */
public class ExpandableParentListItem implements ParentObject {

    private UUID mId;
    private String mTitle;
    private String msubTitle;
    private Date mDate;
    private boolean mSolved;
    private List<ExpandableChildListItem> mChildItemList;
    private List<Object> childObjectList;
    private int mParentNumber;
    private String mParentText;
    private boolean mInitiallyExpanded;
    private IIcon mIcon;
    private int mItemCount = 0;

    private int searchEngineId = 0;

    //HIDDEN DATA
    private String _itemClassName;


    public ExpandableParentListItem() {
        mId = UUID.randomUUID();
        mDate = new Date();
    }

    public int getParentNumber() {
        return mParentNumber;
    }

    public void setParentNumber(int parentNumber) {
        mParentNumber = parentNumber;
    }


    public void setParentText(String parentText) {
        mParentText = parentText;
    }


    public String getParentText() {
        return mParentText;
    }

    public IIcon getIcon() {
        return mIcon;
    }

    public void setIcon(IIcon mIcon) {
        this.mIcon = mIcon;
    }

    public UUID getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getsubTitle() {
        return msubTitle;
    }

    public void setsubTitle(String msubTitle) {
        this.msubTitle = msubTitle;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public void setSolved(boolean solved) {
        mSolved = solved;
    }

    public void setInitiallyExpanded(boolean initiallyExpanded) {
        mInitiallyExpanded = initiallyExpanded;
    }

    public void setChildItemList(List<ExpandableChildListItem> list) {
        mChildItemList = list;
    }


    public List<Object> getChildObjectList(){
        return childObjectList;
    }

    @Override
    public void setChildObjectList(List<Object> list) {
        this.childObjectList = list;
    }

    public String getItemClassName() {
        return _itemClassName;
    }

    public void setItemClassName(String _itemClassName) {
        this._itemClassName = _itemClassName;
    }

    public String getItemCount() {
        return String.valueOf(mItemCount);
    }

    public int getItemCountint() {
        return (mItemCount);
    }

    public void setItemCount(int mItemCount) {
        this.mItemCount = mItemCount;
    }

    public int getSearchEngineId() {
        return searchEngineId;
    }

    public void setSearchEngineId(int searchEngineId) {
        this.searchEngineId = searchEngineId;
    }
}
