package com.example.projetcloudfirebase;

import android.content.ContentValues;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import android.net.Uri;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    StorageReference referenceStorage = FirebaseStorage.getInstance().getReference(); // Réference du Firebase Storage
    private final Handler handler = new Handler();
    private boolean enCours = false; // Booléan qui indique si il faut prendre des photos automatiquement ou pas
    String nom; // Nom de l'image
    private PreviewView previewView;
    private Vector<String> tabUri = null; // Tableau servant à stocker les URL d'images
    private final int intervaleDeCapture = 200; // Intervalle de capture en millisecondes, de base à 500
    ImageCapture imageCapture;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    int compt=0; // Compteur servant à supprimer les images
    int nbStorage=0; // Compteur d'images dans le storage
    ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tabUri = new Vector<String>();
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.cameraPreview);
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);
        Button startCaptureButton = findViewById(R.id.startCaptureButton);
        listenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = listenableFuture.get();
                    startCam(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        },ContextCompat.getMainExecutor(this));
        startCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enCours) {
                    handler.removeCallbacksAndMessages(null);
                    Toast.makeText(MainActivity.this, "Fin", Toast.LENGTH_LONG).show();
                    enCours = false;
                } else {
                    enCours = true;
                    Toast.makeText(MainActivity.this, "Mis en mode capture auto", Toast.LENGTH_LONG).show();
                    captureAuto();
                }
            }
        });
    }
    private void captureAuto() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(enCours)
                {
                capturePhoto();
                }
                handler.postDelayed(this, intervaleDeCapture);
            }
        }, intervaleDeCapture);
    }
    public void startCam(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder().build();
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void capturePhoto(){
        if(imageCapture==null) return;
        Random rand = new Random();
        nom = String.valueOf(System.currentTimeMillis() + rand.nextInt(10000));
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, nom);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXStable");
        }

        OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback(){
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults)
            {
                uploadImage(outputFileResults.getSavedUri());
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception)
            {
                exception.printStackTrace();
            }
        });
    }
    private void deleteImage(String file){
        StorageReference ref = storage.getReferenceFromUrl("gs://testimage-ffe9e.appspot.com/"+file);
        ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {      ;

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }
    private void uploadImage(Uri file) {
        StorageReference ref = referenceStorage.child("images/" + nom);
        ref.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                tabUri.add(ref.getPath());
                nbStorage++;
                if(nbStorage>=60) {
                    deleteImage(tabUri.get(compt));
                    compt++;
                    nbStorage--;
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });}
}