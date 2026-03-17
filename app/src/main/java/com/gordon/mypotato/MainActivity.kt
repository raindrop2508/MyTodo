package com.gordon.mypotato

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gordon.mypotato.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the ViewModel for DataBinding
        binding.viewModel = viewModel
        // Specify the current activity as the lifecycle owner of the binding
        binding.lifecycleOwner = this

        setSupportActionBar(binding.toolbar)
    }
}