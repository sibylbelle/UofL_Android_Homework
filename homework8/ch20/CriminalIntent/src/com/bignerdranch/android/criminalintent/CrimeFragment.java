package com.bignerdranch.android.criminalintent;

import java.util.Date;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;

public class CrimeFragment extends Fragment {
	private static final String TAG = "CrimeFragment";
	public static final String EXTRA_CRIME_ID = "com.bignerdranch.android.criminalintent.crime_id"; 
	private static final String DIALOG_IMAGE = "image"; 
	
	private static final String DIALOG_DATE = "date"; 
	private static final int REQUEST_DATE = 0; 
	private static final int REQUEST_PHOTO = 1; 
	private Crime mCrime; 
	private EditText mTitleField; 
	private Button mDateButton; 
	//private CheckBox mSolvedCheckBox; 
	private ImageButton mPhotoButton; 
	private ImageView mPhotoView;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		UUID crimeId = (UUID)getActivity().getIntent().getSerializableExtra(EXTRA_CRIME_ID); 
		
		mCrime = CrimeLab.get(getActivity()).getCrime(crimeId); 
		
		setHasOptionsMenu(true); 
	}
	
	public void updateDate() {
		mDateButton.setText(mCrime.getDate().toString()); 
	}
	
	@TargetApi(11)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_crime, parent, false);
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if(NavUtils.getParentActivityName(getActivity()) != null) {
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(true); 
			}
		}	
		
		mTitleField = (EditText)v.findViewById(R.id.crime_title); 
		mTitleField.setText(mCrime.getTitle()); 
		mTitleField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, int count) {
				mCrime.setTitle(c.toString());
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after) {
				//Intentional blank
			}
			
			public void afterTextChanged(Editable c) {
				//Blank
			}
		});
		
		mDateButton = (Button)v.findViewById(R.id.crime_date); 
		mDateButton.setText(mCrime.getDate().toString()); 
		mDateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				FragmentManager fm = getActivity().getSupportFragmentManager(); 
				DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate()); 
				dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE); 
				dialog.show(fm, DIALOG_DATE); 
			}
		});
		
		
		/*mSolvedCheckBox = (CheckBox)v.findViewById(R.id.crime_solved); 
		 mSolvedCheckBox.setChecked(mCrime.isSolved()); 
		 mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangedListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// set crime's solved property
				mCrime.setSolved(isChecked); 
			}
		});
		*/
		
		mPhotoButton = (ImageButton)v.findViewById(R.id.crime_imageButton); 
		mPhotoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), CrimeCameraActivity.class); 
				startActivityForResult(i, REQUEST_PHOTO);
			}
		});
		
		mPhotoView = (ImageView)v.findViewById(R.id.crime_imageView);
		mPhotoView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Photo p = mCrime.getPhoto();
				if(p == null) 
					return; 
				
				FragmentManager fm = getActivity().getSupportFragmentManager();
				String path = getActivity().getFileStreamPath(p.getFilename()).getAbsolutePath();
				ImageFragment.newInstance(path).show(fm, DIALOG_IMAGE);
			}
		});
		
		PackageManager pm = getActivity().getPackageManager(); 
		if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			mPhotoButton.setEnabled(false);
		}
			
		return v; 
	}
	
	private void showPhoto() {
		Photo p = mCrime.getPhoto(); 
		BitmapDrawable b = null; 
		if (p != null) {
			String path = getActivity().getFileStreamPath(p.getFilename()).getAbsolutePath(); 
			b = PictureUtils.getScaledDrawable(getActivity(), path); 
		}
		mPhotoView.setImageDrawable(b);
	}
	
	@Override
	public void onStart() {
		super.onStart(); 
		showPhoto();
	}
	
	@Override 
	public void onStop() {
		super.onStop(); 
		PictureUtils.cleanImageView(mPhotoView); 
	}

	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode != Activity.RESULT_OK) return; 
		if(requestCode == REQUEST_DATE) {
			Date date = (Date)data.getSerializableExtra(DatePickerFragment.EXTRA_DATE); 
			mCrime.setDate(date); 
			updateDate(); 
		} else if (requestCode == REQUEST_PHOTO) {
			String filename = data.getStringExtra(CrimeCameraFragment.EXTRA_PHOTO_FILENAME); 
			if (filename != null) {
				Photo p = new Photo(filename); 
				mCrime.setPhoto(p); 
				showPhoto();
			}
		}
	}
	
	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: 
			if(NavUtils.getParentActivityName(getActivity()) != null) {
				NavUtils.navigateUpFromSameTask(getActivity()); 
			}
			return true; 
			default: 
				return super.onOptionsItemSelected(item); 
		}
	}
	
	@Override
	public void onPause() {
		super.onPause(); 
		CrimeLab.get(getActivity()).saveCrimes(); 
	}
	
	public static CrimeFragment newInstance(UUID crimeId) {
		Bundle args = new Bundle(); 
		args.putSerializable(EXTRA_CRIME_ID,  crimeId); 
		
		CrimeFragment fragment = new CrimeFragment(); 
		fragment.setArguments(args); 
		
		return fragment; 
	}

}
