package com.example.cookpal

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

class CalculatorFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calculator, container, false)
    }
}
