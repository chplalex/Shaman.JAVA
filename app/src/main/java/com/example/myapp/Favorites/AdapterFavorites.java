package com.example.myapp.Favorites;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.DBService.Location;
import com.example.myapp.DBService.ShamanDao;
import com.example.myapp.MainApp;
import com.example.myapp.R;
import com.example.myapp.WeatherData.WeatherData;
import com.example.myapp.WeatherService.OpenWeatherRetrofit;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.example.myapp.Common.Utils.LOCATION_ARG_COUNTRY;
import static com.example.myapp.Common.Utils.LOCATION_ARG_NAME;
import static com.example.myapp.Common.Utils.LOGCAT_TAG;
import static com.example.myapp.WeatherService.OpenWeatherRetrofit.APP_ID;
import static com.example.myapp.WeatherService.OpenWeatherRetrofit.BASE_URL;

public class AdapterFavorites extends RecyclerView.Adapter<AdapterFavorites.ViewHolder> {

    private ShamanDao shamanDao;
    private List<Location> locations;
    private OpenWeatherRetrofit openWeatherRetrofit;
    private String lang;
    private String units;

    public AdapterFavorites(SharedPreferences sharedPreferences) {
        shamanDao = MainApp.getInstance().getShamanDao();
        locations = shamanDao.getFavoriteLocations();
        Log.d(LOGCAT_TAG, "locations.size() = " + locations.size());
        openWeatherRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenWeatherRetrofit.class);
        lang = sharedPreferences.getString("pref_lang", "RU");
        units = sharedPreferences.getString("pref_units", "metric");
    }

    @NonNull
    @Override
    public AdapterFavorites.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_locations_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterFavorites.ViewHolder viewHolder, int i) {
        Location location = locations.get(i);
        viewHolder.requestOpenWeatherRetrofit(location.name, location.country);
    }

    public int getItemCount() { return locations.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private View view;
        private MaterialTextView txtLocationName;
        private MaterialTextView txtLocationCountry;
        private MaterialTextView txtTemperature;
        private ShapeableImageView imgWeatherIcon;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            txtLocationName = itemView.findViewById(R.id.txtLocationName);
            txtLocationCountry = itemView.findViewById(R.id.txtLocationCountry);
            imgWeatherIcon = itemView.findViewById(R.id.imgWeatherIcon);
            txtTemperature = itemView.findViewById(R.id.txtTemperature);
            view.setOnClickListener((View v) -> {
                Bundle bundle = new Bundle();
                bundle.putCharSequence(LOCATION_ARG_NAME, txtLocationName.getText());
                bundle.putCharSequence(LOCATION_ARG_COUNTRY, txtLocationCountry.getText());
                Navigation.findNavController(view).navigate(R.id.actionStart, bundle);
            });
        }

        private void requestOpenWeatherRetrofit(String locationName, String locationCountry) {

            openWeatherRetrofit.loadWeather(locationName + "," + locationCountry, APP_ID, lang, units).enqueue(new Callback<WeatherData>() {
                @Override
                public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        initViewsByGoodResponse(response.body());
                    } else {
                        initViewsByFailResponse();
                    }
                }

                @Override
                public void onFailure(Call<WeatherData> call, Throwable t) {
                    initViewsByFailResponse();
                }
            });
        }

        private void initViewsByGoodResponse(@NotNull WeatherData wd) {
            wd.setResources(view.getResources());
            txtLocationName.setText(wd.getName());
            txtLocationCountry.setText(wd.getCountry());
            imgWeatherIcon.setImageResource(wd.getImageResource());
            txtTemperature.setText(wd.getTemperature());
        };

        private void initViewsByFailResponse() {
            txtLocationName.setText(view.getResources().getString(R.string.not_found_location_name));
            txtLocationCountry.setText(view.getResources().getString(R.string.not_found_location_country));
            imgWeatherIcon.setImageResource(R.drawable.ic_report_problem);
            txtTemperature.setText(view.getResources().getString(R.string.not_found_location_temp));
        }
    }
}