package com.belkin.linker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    final static String CLASS_LOG_TAG = "MainActivity";

    private final int REQUEST_CODE_PERMISSIONS = 42;
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
    };


    List<Link> displayedLinks;
    LinkToListItemAdapter adapter;
    ConstraintLayout rootLayout;
    ItemTouchHelper.SimpleCallback itemTouchHelperCallback;
    RecyclerView recyclerView;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) == PackageManager.PERMISSION_DENIED)
                return false;
        }
        return true;
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        onListChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(CLASS_LOG_TAG, "onCreate(Bundle) method call");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //download ML model if it's not downloaded yet
        Analyzer.warmUp();


        //DataBase
        DataBase.setContext(getBaseContext());
        DataBase.readAllDataBase();
        displayedLinks = DataBase.getData();
        MyRecyclerItemTouchHelperListener listener = new MyRecyclerItemTouchHelperListener();
        adapter = new LinkToListItemAdapter(this, displayedLinks, listener);
        DataBase.setAdapter(adapter);

        //ActionBar
        getSupportActionBar().hide();

        //Root Layout
        rootLayout = (ConstraintLayout) findViewById(R.id.main_root);

        //RecycleView
        recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, listener);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        //scan btn
        ImageButton scanBtn = (ImageButton) findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), CameraActivity.class);
            startActivity(intent);
            Log.i(CLASS_LOG_TAG, "CameraActivity started");
        });

        //todo: search view
        //searchView
        SearchView searchView = (SearchView) findViewById(R.id.searchView);

        onListChanged();


        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }


    public void onListChanged() {
        Log.i(CLASS_LOG_TAG, "onListChanged() method call");
        if (adapter.getItemCount() == 0) {
            Log.i(CLASS_LOG_TAG, "List is empty. Showing info icon and text");
            findViewById(R.id.empty_list_image).setVisibility(View.VISIBLE);
            findViewById(R.id.empty_list_description1).setVisibility(View.VISIBLE);
            findViewById(R.id.empty_list_description2).setVisibility(View.VISIBLE);
        } else {
            Log.i(CLASS_LOG_TAG, "List is not empty. Hiding info icon and text");
            findViewById(R.id.empty_list_image).setVisibility(View.INVISIBLE);
            findViewById(R.id.empty_list_description1).setVisibility(View.INVISIBLE);
            findViewById(R.id.empty_list_description2).setVisibility(View.INVISIBLE);
        }
    }

    class MyRecyclerItemTouchHelperListener implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
        final static String CLASS_LOG_TAG = MainActivity.CLASS_LOG_TAG + ".TchListnr";

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
            Log.i(CLASS_LOG_TAG, "onSwiped() method call");
            if (viewHolder instanceof LinkToListItemAdapter.ViewHolder) {
                switch (direction) {
                    case ItemTouchHelper.LEFT:
                        Log.i(CLASS_LOG_TAG, "Left swipe performed");
                        // get the removed item name to display it in snack bar
                        String name = displayedLinks.get(viewHolder.getAdapterPosition()).getHost();

                        // backup of removed item for undo purpose
                        final Link deletedItem = displayedLinks.get(viewHolder.getAdapterPosition());
                        final int deletedIndex = viewHolder.getAdapterPosition();

                        // remove the item from recycler view
                        adapter.removeItem(viewHolder.getAdapterPosition());
                        DataBase.deleteFromDataBase(deletedItem);
                        onListChanged();
                        Log.i(CLASS_LOG_TAG, "Item deleted: " + deletedItem.toString());

                        // showing snack bar with Undo option
                        Snackbar snackbar = Snackbar.make(rootLayout, name + " removed from list!", Snackbar.LENGTH_LONG);
                        snackbar.setAction("UNDO", view -> {
                            // undo is selected, restore the deleted item
                            adapter.restoreItem(deletedItem, deletedIndex);
                            DataBase.writeToDataBase(deletedItem);
                            onListChanged();
                            Log.i(CLASS_LOG_TAG, "Item restored: " + deletedItem.toString());
                        });
                        snackbar.setActionTextColor(Color.YELLOW);

                        snackbar.show();
                        break;
                    case ItemTouchHelper.RIGHT:
                        Log.i(CLASS_LOG_TAG, "Right swipe performed");
                        String url = displayedLinks.get(viewHolder.getAdapterPosition()).getUrl();
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT, url);
                        startActivity(Intent.createChooser(share, "Share link"));
                        Log.i(CLASS_LOG_TAG, "Share activity started");

                        final Link deletedItem1 = displayedLinks.get(viewHolder.getAdapterPosition());
                        final int deletedIndex1 = viewHolder.getAdapterPosition();
                        adapter.removeItem(viewHolder.getAdapterPosition());
                        adapter.restoreItem(deletedItem1, deletedIndex1);

                        break;
                }
            }
        }

        @Override
        public void onClicked(RecyclerView.ViewHolder viewHolder) {
            Log.i(CLASS_LOG_TAG, "onClicked() method call");
            String url = displayedLinks.get(viewHolder.getAdapterPosition()).getUrl();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            Log.i(CLASS_LOG_TAG, "Browser activity started");
        }
    }

}


