package uk.org.ngo.squeezer.framework.expandable;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.org.ngo.squeezer.model.ExpandableParentListItem;

public class CrimeLab {
    private static CrimeLab sCrimeLab;

    private ArrayList<ExpandableParentListItem> mCrimes;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    private CrimeLab(Context context) {
        mCrimes = new ArrayList<>();
        for (int i = 0; i < 0; i++) {
            ExpandableParentListItem crime = new ExpandableParentListItem();
            crime.setTitle(String.format("Groeps titel %d", i));
            crime.setSolved(i % 2 == 0);
            mCrimes.add(crime);
        }
    }

    public List<ExpandableParentListItem> getCrimes() {
        return mCrimes;
    }

    public ExpandableParentListItem getCrime(UUID id) {
        for (ExpandableParentListItem crime : mCrimes) {
            if (crime.getId().equals(id)) {
                return crime;
            }
        }
        return null;
    }

    public void setCrime(ExpandableParentListItem object){
        mCrimes.add(object);
    }
}
