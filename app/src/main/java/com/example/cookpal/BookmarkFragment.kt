package com.example.cookpal

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

class BookmarkFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_bookmark, container, false)
    }
}
