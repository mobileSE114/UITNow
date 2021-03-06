package com.uit.uitnow;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class StoreActivity extends AppCompatActivity implements View.OnClickListener,ItemAdapter.OnItemClickListener {
    TextView tvName, tvAddress, tvOpenHours, tvTotalPrices, tvTotalItems;
    ImageView ivCover,btnLike;
    View layoutViewBasket;
    RecyclerView rvDrinks;
    ItemAdapter itemAdapter;
    Store store;
    App app;
    FirebaseFirestore db;
    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent event)
    {
        if (event != null && MessageEvent.FROM_storeFRAG_TO_storeACT.equals(event.type)) {
            store=event.store;
            store.menu=new ArrayList<>();
            app.basket = new Basket();
            displayStoreInfo();
            showMenu(store.id);
            EventBus.getDefault().removeStickyEvent(event);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        db=FirebaseFirestore.getInstance();
        app= (App) getApplication();
//        listRestaurant=Restaurant.getMockData();
        //  restaurant = new Restaurant("7 Eleven", R.drawable.ic_seveneleven, R.drawable.cover_menu_1, "82 Nguyen Thi Minh Khai, Phuong 6, Quan 3", "6:00 - 23:00");
        rvDrinks = findViewById(R.id.rvFoods);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvDrinks.setLayoutManager(layoutManager);
        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        tvOpenHours = findViewById(R.id.tvOpenHours);
        ivCover = findViewById(R.id.ivCover);
        tvTotalPrices = findViewById(R.id.tvTotalPrices);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        layoutViewBasket = findViewById(R.id.layoutViewBasket);
        layoutViewBasket.setOnClickListener(this);
        btnLike=findViewById(R.id.btnLike);
        btnLike.setOnClickListener(this);
    }

    private void displayStoreInfo() {
        tvName.setText(store.name);
        tvAddress.setText(store.address);
        tvOpenHours.setText(store.getOpenHours());
        btnLike.setImageDrawable(app.savedStore.contains(store.getId())?getResources().getDrawable(R.drawable.icon_liked):getResources().getDrawable(R.drawable.icon_like));
        Picasso.get().load(store.getLogoUrl()).into(ivCover);
    }
    private void showMenu(String id)
    {
        db.collection("Stores").document(id).collection("menu").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Item item=document.toObject(Item.class);
                                store.menu.add(item);
                                Log.d("Test", document.getId() + " => " + document.getData());
                            }
                            Log.e("Test","Number of stores: "+store.menu.size());
                            DividerItemDecoration dividerItemDecoration=new DividerItemDecoration(StoreActivity.this,DividerItemDecoration.VERTICAL);
                            dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider_1));
                            itemAdapter = new ItemAdapter(store.menu,StoreActivity.this);
                            rvDrinks.addItemDecoration(dividerItemDecoration);
                            rvDrinks.setAdapter(itemAdapter);
                        } else {
                            Log.d("Test", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.layoutViewBasket)
        {
            if (app.basket.getTotalItem() > 0) {
                BasketDialogFragment dialog = new BasketDialogFragment();
                app.basket.calculateBasket();
                tvTotalPrices.setText(app.basket.getTotalPrice());
                tvTotalItems.setText(app.basket.getTotalItem() + "");
                dialog.show(getSupportFragmentManager(), "basket_dialog");
            } else {
                Toast.makeText(this, "Giỏ hàng đang trống",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if(view.getId()==R.id.btnLike)
        {
            if(!app.savedStore.contains(store.getId()))
            {
                Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();
                btnLike.setImageDrawable(getResources().getDrawable(R.drawable.icon_liked));
                app.savedStore.add(store.getId());
            }
            else
            {
                Toast.makeText(this,"Unsaved",Toast.LENGTH_SHORT).show();
                btnLike.setImageDrawable(getResources().getDrawable(R.drawable.icon_like));
                app.savedStore.remove(store.getId());
            }
        }
    }

    @Override
    public void onItemClick(Item item) {
        ItemBasket itemBasket = app.basket.getItem(item.id); // 1
        if (itemBasket == null) // 2
            itemBasket = new ItemBasket(item, 1, item.price,"");

        AddToBasketDialogFragment dialog = new AddToBasketDialogFragment(itemBasket);
        dialog.show(getSupportFragmentManager(), "add_to_basket_dialog"); // 3
    }

    public void updateBasket() {
        app.basket.calculateBasket();
        tvTotalPrices.setText(app.basket.getTotalPrice());
        tvTotalItems.setText(app.basket.getTotalItem() + "");
    }

    public void openOrderTrackingActivity() {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        startActivity(intent);
        EventBus.getDefault().postSticky(new MessageEvent(app.request.storeLocation,MessageEvent.FROM_storeACT_TO_trackingACT));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void requestOrder(GeoPoint store)
    {
        app.request = new OrderRequest();
        app.request.userId = app.user.id;
        app.request.userName = app.user.name;
        app.request.userAddress = app.user.address;
        app.request.userLocation=app.location;
        app.request.storeName = this.store.name;
        app.request.storeAddress = this.store.address;
        app.request.storeLocation=store;
        app.request.total = app.basket.getTotalPrice();
        app.request.idOrder=app.order.getId();
        app.request.userPhone=app.user.phone;
        db.collection("Requests").add(app.request).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                app.requestId = documentReference.getId();
                Log.e("Test:",   " request id: " + app.requestId);
                app.request.id = documentReference.getId();
                PrefUtil.savePref(StoreActivity.this, "request_id", app.requestId);
                updateRequestId();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

    private void updateRequestId() {
        db.collection("Requests").document(app.requestId).set(app.request).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                        openOrderTrackingActivity();
            }
        });
    }
}
