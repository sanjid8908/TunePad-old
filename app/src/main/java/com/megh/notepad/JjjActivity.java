package com.megh.notepad;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.arch.core.*;
import androidx.asynclayoutinflater.*;
import androidx.coordinatorlayout.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.*;
import androidx.cursoradapter.*;
import androidx.customview.*;
import androidx.documentfile.*;
import androidx.drawerlayout.*;
import androidx.exifinterface.*;
import androidx.fragment.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.*;
import androidx.legacy.coreui.*;
import androidx.legacy.coreutils.*;
import androidx.lifecycle.*;
import androidx.lifecycle.livedata.*;
import androidx.lifecycle.livedata.core.*;
import androidx.lifecycle.viewmodel.*;
import androidx.loader.*;
import androidx.localbroadcastmanager.*;
import androidx.print.*;
import androidx.slidingpanelayout.*;
import androidx.swiperefreshlayout.*;
import androidx.versionedparcelable.*;
import androidx.viewpager.*;
import com.google.android.datatransport.*;
import com.google.android.datatransport.backend.cct.*;
import com.google.android.datatransport.runtime.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.odml.image.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import com.google.mlkit.common.*;
import com.google.mlkit.vision.common.*;
import com.google.mlkit.vision.interfaces.*;
import com.google.mlkit.vision.text.bundled.common.*;
import com.google.mlkit.vision.text.bundled.latin.*;
import com.megh.notepad.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class JjjActivity extends AppCompatActivity {
	
	private JjjBinding binding;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = JjjBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		setSupportActionBar(binding.Toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		binding.Toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
	}
	
	private void initializeLogic() {
	}
	
}