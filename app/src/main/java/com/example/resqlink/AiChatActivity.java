package com.example.resqlink;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatActivity extends AppCompatActivity {


    private static final String API_KEY = "AIzaSyCaSW41XpAr4CsbOu88p_oPos747O6v9S0";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TextView responseTextView;

    FloatingActionButton submitButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.AiChatActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        responseTextView = findViewById(R.id.responsiveTextView);
        submitButton = findViewById(R.id.query_submit_button);

        AutoCompleteTextView queryInput = findViewById(R.id.queryInput);

// Sample dropdown suggestions
        String[] suggestions = new String[] {
                "How can i reduce health risk level and what actions should I take?",
                "What precautions should I take during a fever?",
                "How do I know if it's a medical emergency?",
                "What are the symptoms of a heart attack?",
                "How can I treat a minor burn at home?",
                "Which medicine should I take for a headache?"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
        );

// Set the adapter to the AutoCompleteTextView
        queryInput.setAdapter(adapter);

// Optional: show dropdown immediately when clicked
        queryInput.setOnClickListener(v -> queryInput.showDropDown());

        //sendGeminiRequest();
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(queryInput.getText().equals("")){
                    Toast.makeText(AiChatActivity.this, "Please Select a Query", Toast.LENGTH_SHORT).show();
                    submitButton.hide();  // for smooth animation (preferred)
                    submitButton.setVisibility(View.GONE); // or View.INVISIBLE
                }
                else{
                    // To show
                    submitButton.show();  // with animation
                    submitButton.setVisibility(View.VISIBLE);
                    String message = queryInput.getText().toString();
                    message = "           \"text\": \"Explain {"+message+"} in short and simple language \"\n";
                    sendGeminiRequest(message);
                }
            }
        });

    }
    private void sendGeminiRequest(String message) {
        OkHttpClient client = new OkHttpClient();


        String jsonBody = "{\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"parts\": [\n" +
                "        {\n" +
                message+
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("GeminiAPI", "Request failed", e);
                runOnUiThread(() -> responseTextView.setText("Request failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    Log.d("GeminiAPI", "Response: " + responseData);

                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray candidates = jsonObject.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        final String aiReply = parts.getJSONObject(0).getString("text");

                        final String cleanedReply = aiReply.replaceAll("\\*", "");

                        // Optional: Trim extra spaces and tidy it up
                        final String cleanedReply2 = cleanedReply.replaceAll(" +", " ").trim();
                        // Show the response in the TextView
                        runOnUiThread(() -> responseTextView.setText(cleanedReply2));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> responseTextView.setText("Parsing error: " + e.getMessage()));
                    }
                } else {
                    runOnUiThread(() -> responseTextView.setText("Unexpected response code: " + response.code()));
                }
            }
        });
    }
}
