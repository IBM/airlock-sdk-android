package com.ibm.airlock.sdk.ui;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.R;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class FeaturesListFragment extends ListFragment {

    protected HashMap<String, Feature> originalFeatures = new HashMap<>();
    protected List<Feature> filteredFeatures = new ArrayList<Feature>();
    EditText searchedTxtView;
    String searchedTxt;
    protected ListView featuresListView;
    //adapter for rendering
    private ArrayAdapter<Feature> adapter;
    private OnFeatureSelectedListener mCallback;

    public FeaturesListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        featuresListView = view.findViewById(R.id.feature_list);
        searchedTxtView = view.findViewById(R.id.search_bar);

        adapter = new AirlockFeatureAdapter(getActivity(),
                android.R.layout.simple_list_item_1, filteredFeatures);

        featuresListView.setAdapter(adapter);

        searchedTxtView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                adapter.getFilter().filter(sequence);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_features_list, container, false);
        return view;
    }

    private void addAllFeaturesAndSub(List<Feature> rootFeatures) {
        for (Feature feature : rootFeatures) {
            if (!originalFeatures.containsKey(feature.getName())) {
                FeaturesListFragment.this.originalFeatures.put(feature.getName(), feature);
                FeaturesListFragment.this.filteredFeatures.add(feature.clone());
            }
            addAllFeaturesAndSub(feature.getChildren());
        }
    }


//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        if (mCallback != null && adapter != null) {
//            mCallback.onFeatureSelected(adapter.getItem(position));
//        }
//    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFeatureSelectedListener) {
            mCallback = (OnFeatureSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFeatureSelectedListener");
        }
        try {
            AirlockManager.getInstance().getCacheManager().clearTimeStamps();
            AirlockManager.getInstance().pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(this.getClass().getName(), "Failed to pull Airlock features");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "Failed to pull Airlock features ", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onSuccess(@NonNull String msg) {
                    Log.d(this.getClass().getName(), "Pull features is Done");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "Pull features is Done", Toast.LENGTH_SHORT).show();
                                updateListData();
                            }
                        });
                    }
                }
            });
        } catch (AirlockNotInitializedException e) {
            Log.e(this.getClass().getName(), "Airlock pull Failed: " + e.getLocalizedMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity().getApplicationContext(), "Failed to pull Airlock features ", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        originalFeatures.clear();
        filteredFeatures.clear();
        mCallback = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle();
        if (searchedTxt != null && !searchedTxt.isEmpty()) {
            searchedTxtView.setText(searchedTxt);
        } else {
            searchedTxtView.setText("");
        }
    }

    protected void setTitle() {
        getActivity().setTitle("Features");
    }

    public void updateListData() {
        if (originalFeatures != null && !originalFeatures.isEmpty()) {
            originalFeatures.clear();
        }
        if (filteredFeatures != null && !filteredFeatures.isEmpty()) {
            filteredFeatures.clear();
        }
        try {
            AirlockManager.getInstance().calculateFeatures(((DebugFeaturesActivity) getActivity()).getDeviceContext(), AirlockManager.getInstance().getPurchasedProductIdsForDebug());
            AirlockManager.getInstance().syncFeatures();
        } catch (AirlockNotInitializedException | JSONException e) {
            Toast.makeText(getActivity().getApplicationContext(), "Failed to calculate : " + e.toString(), Toast.LENGTH_LONG).show();
            Log.d(this.getClass().getName(), "Airlock calculate & Sync Failed: " + e.getLocalizedMessage());
        }
        addAllFeatures();
        if (adapter != null) {
            if (searchedTxtView.getText() != null) {
                adapter.getFilter().filter(searchedTxtView.getText());
            } else {
                adapter.getFilter().filter("");
            }
        }
    }

    protected void addAllFeatures() {
        addAllFeaturesAndSub(AirlockManager.getInstance().getRootFeatures());
    }

    @Override
    public void onPause() {
        super.onPause();
        searchedTxt = searchedTxtView.getText().toString();
    }

    public void refreshAfterActivityAction() {
        originalFeatures.clear();
        filteredFeatures.clear();
        addAllFeatures();
        adapter.getFilter().filter(searchedTxtView.getText());
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFeatureSelectedListener {
        void onFeatureSelected(Feature feature);
    }


    static class ViewHolder {
        TextView textView;
    }

    //*********** Inner Class - AirlockListAdapter *******************//
    public class AirlockFeatureAdapter extends ArrayAdapter<Feature> {
        public AirlockFeatureAdapter(Context context, int resource, List<Feature> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, @Nullable View convertView, ViewGroup parent) {

            ViewHolder holder;


            if (convertView == null) {
                convertView = new TextView(getContext());
                convertView.setPadding(20, 30, 20, 30);
                ((TextView) convertView).setTextSize(15);
                holder = new ViewHolder();
                holder.textView = (TextView) convertView;
                convertView.setTag(holder);

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCallback != null && adapter != null) {
                            mCallback.onFeatureSelected(adapter.getItem(position));
                        }
                    }
                });

            } else {
                /* We recycle a View that already exists */
                holder = (ViewHolder) convertView.getTag();
            }

            Feature item = getItem(position);
            if (item != null) {
                if (item.isOn()) {
                    holder.textView.setTextColor(Color.BLUE);
                } else {
                    holder.textView.setTextColor(Color.BLACK);
                }
                holder.textView.setText(item.getName());
            }


            return convertView;
        }

        @Override
        public Filter getFilter() {

            Filter filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredFeatures.clear();

                    if (results != null && results.values != null) {
                        filteredFeatures.addAll((List<Feature>) results.values);
                    }

                    Collections.sort(filteredFeatures, new Comparator<Feature>() {
                        @Override
                        public int compare(Feature feature, Feature t1) {
                            return feature.getName().compareToIgnoreCase(t1.getName());
                        }
                    });
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<Feature> filteredArray = new ArrayList<>();

                    // perform your search here using the searchConstraint String.
                    constraint = constraint.toString().toLowerCase(Locale.getDefault());
                    for (Feature feature : originalFeatures.values()) {
                        if (feature.getName().toLowerCase(Locale.getDefault()).contains(constraint)) {
                            filteredArray.add(feature.clone());
                        }
                    }
                    results.count = filteredArray.size();
                    results.values = filteredArray;
//                    Log.e("VALUES", results.values.toString());
                    return results;
                }
            };

            return filter;
        }
    }
}
