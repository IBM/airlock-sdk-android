package com.weather.airlock.sdk.ui;

import java.util.Collection;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.data.Servers;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;


public class AirlockSelectServerActivity extends AppCompatActivity {

    private static final String TAG = "AirlockServer";
    private final int SERVER_MODE = 0;
    private final int PRODUCT_MODE = 1;
    private ListView listView;
    private ArrayAdapter<Servers.Server> adapter;
    private ProgressDialog loading;
    private Servers.Server selectedServer;
    private int MODE;
    private ViewGroup productHeader;
    private ViewGroup serverHeader;
    private Servers servers;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MODE = SERVER_MODE;
        //set empty list
        setContentView(R.layout.airlock_server_list_layout);

        this.servers = AirlockManager.getInstance().getCacheManager().getServers();

        //init UI references
        findViewsById();
        listView.addHeaderView(serverHeader);

        adapter = new AirlockListAdapter(AirlockSelectServerActivity.this);
        //new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // this is the header
                    return;
                }
                Object item = adapter.getItem(position - 1);
                if (item instanceof Servers.Server) {
                    selectedServer = (Servers.Server) item;
                    //title.setText(selectedServer.getDisplayName());
                    getSelectedServerProducts();
                    listView.removeHeaderView(serverHeader);
                    adapter.clear();
                } else if (item instanceof Servers.Product) {
                    selectProduct((Servers.Product) item);
                }
            }
        });
        loading = new ProgressDialog(this);
        loading.setMessage("Loading ...");
        loading.setCancelable(true);

        getServers();
    }

    @Override
    public void onBackPressed() {
        if (MODE == SERVER_MODE) {
            super.onBackPressed();
        } else {
            setMode(SERVER_MODE);
        }
    }

    @Override
    protected void onDestroy() {
        dismissLoadingDialog();
        super.onDestroy();
    }

    private void setMode(int mode) {
        switch (mode) {
            case SERVER_MODE:
                MODE = SERVER_MODE;
                listView.removeHeaderView(productHeader);
                listView.addHeaderView(serverHeader);
                setTitle("Server");
                adapter.clear();
                adapter.addAll(AirlockManager.getInstance().getCacheManager().getServers().getList().values());
                adapter.notifyDataSetChanged();
                break;
            case PRODUCT_MODE:
                MODE = PRODUCT_MODE;
                listView.removeHeaderView(serverHeader);
                listView.addHeaderView(productHeader);
                break;
        }
    }

    private void findViewsById() {
        listView = (ListView) findViewById(R.id.list);
        LayoutInflater inflater = getLayoutInflater();
        serverHeader = (ViewGroup) inflater.inflate(R.layout.servers_header, listView, false);
        productHeader = (ViewGroup) inflater.inflate(R.layout.products_header, listView, false);
    }

    private void selectProduct(final Servers.Product item) {

        //Loading
        showLoadingDialog();
        // Download default - the download will save product ID, serverId and Default if success.
        AirlockManager.getInstance().getCacheManager().pullDefaultFile(selectedServer, item, new AirlockCallback() {
            @Override
            public void onFailure(@NonNull final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = e.getMessage();
                        Log.d(TAG, msg, e);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                        dismissLoadingDialog();
                    }
                });
            }

            @Override
            public void onSuccess(@NonNull String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Product [" + item.getName() + "] was successfully set to default", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        });
    }


    private void getSelectedServerProducts() {
        showLoadingDialog();
        AirlockManager.getInstance().getCacheManager().pullProductList(selectedServer, new AirlockCallback() {
            @Override
            public void onFailure(@NonNull final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Failed to product server list", e);
                        Toast.makeText(getApplicationContext(), "Failed to update product list", Toast.LENGTH_LONG).show();
                        loading.dismiss();
                    }
                });
            }

            @Override
            public void onSuccess(@NonNull String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingDialog();
                        Collection products = selectedServer.getProducts();
                        listView.addHeaderView(productHeader);
                        setTitle(selectedServer.getDisplayName() + " Products");
                        MODE = PRODUCT_MODE;
                        if (products != null) {
                            adapter.addAll(products);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }


    private void getServers() {
        showLoadingDialog();
        AirlockManager.getInstance().getCacheManager().pullServerList(new AirlockCallback() {
            @Override
            public void onFailure(@NonNull final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Failed to update server list", e);
                        Toast.makeText(getApplicationContext(), "Failed to update server list", Toast.LENGTH_LONG).show();
                        dismissLoadingDialog();
                    }
                });
            }

            @Override
            public void onSuccess(@NonNull String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingDialog();
                        adapter.clear();
                        adapter.addAll(servers.getList().values());
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private void showLoadingDialog() {
        if (loading != null && !loading.isShowing()) {
            loading.show();
        }
    }

    private void dismissLoadingDialog() {
        if (loading != null && loading.isShowing()) {
            loading.dismiss();
        }
    }

    //*********** Inner Class - AirlockListAdapter *******************//
    public class AirlockListAdapter extends ArrayAdapter {

        private final String defaultPrefix = " (Default)";

        public AirlockListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(getContext());
                convertView.setPadding(20, 30, 20, 30);
                ((TextView) convertView).setTextSize(15);
                ((TextView) convertView).setTextColor(Color.BLACK);
            }
            Object item = getItem(position);

            if (item != null) {
                ((TextView) convertView).setText(getItemName(item));
                if (isItemSelected(item)) {
                    ((TextView) convertView).setTypeface(null, Typeface.BOLD);
                } else {
                    ((TextView) convertView).setTypeface(null, Typeface.NORMAL);
                }
            }
            return convertView;
        }

        private boolean isItemSelected(Object item) {
            if (item instanceof Servers.Server) {
                Servers.Server server = (Servers.Server) item;
                return server.getDisplayName().equals(servers.getCurrentServer().getDisplayName());
            } else if (item instanceof Servers.Product) {
                Servers.Product product = (Servers.Product) item;
                Servers.Server server = product.getServer();
                String currentProdId = AirlockManager.getInstance().getCacheManager().getPersistenceHandler().read(Constants.SP_CURRENT_PRODUCT_ID, "");
                return ((Servers.Product) item).getProductId().equals(currentProdId) && server.getDisplayName().equals(servers.getCurrentServer().getDisplayName());
            }
            return false;
        }

        private String getItemName(Object item) {
            if (item instanceof Servers.Server) {
                Servers.Server server = (Servers.Server) item;
                String name = server.getDisplayName();
                if (name.equals(servers.getDefaultServer().getDisplayName())) {
                    return name + defaultPrefix;
                }
                return name;
            } else if (item instanceof Servers.Product) {
                Servers.Product product = (Servers.Product) item;
                String id = product.getProductId();
                String defaultProdId = AirlockManager.getInstance().getCacheManager().getPersistenceHandler().read(Constants.SP_DEFAULT_PRODUCT_ID, "");
                if (id.equals(defaultProdId) && product.getServer().getDisplayName().equals(servers.getDefaultServer().getDisplayName())) {
                    return product.getName() + defaultPrefix;
                }
                return product.getName();
            }
            return "";
        }
    }
}
