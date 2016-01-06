package uk.org.ngo.squeezer.model;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.Date;
import java.util.List;

/**
 * Created by Stefan on 6-1-2016.
 */
public class ExpandableChildListItem{

    private Date mDate;
    private boolean mSolved;

    private String text1;
    private String text2;
    private int image;

    public ExpandableChildListItem(int img, String t1, String t2) {
        image = img;
        text1 = t1;
        text2 = t2;
    }

    /**
     * Getter and setter methods
     */

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getText1() {
        return text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    public String getText2() {
        return text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }
}
