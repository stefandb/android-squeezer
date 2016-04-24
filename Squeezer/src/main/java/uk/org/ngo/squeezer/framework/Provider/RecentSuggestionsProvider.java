package uk.org.ngo.squeezer.framework.Provider;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by Stefan on 17-4-2016.
 */
public class RecentSuggestionsProvider extends SearchRecentSuggestionsProvider {

    public final static String AUTHORITY = "br.com.edsilfer.content_provider.RecentSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public RecentSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}