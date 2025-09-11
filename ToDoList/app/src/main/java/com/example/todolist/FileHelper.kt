package com.example.todolist

import android.content.Context
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class FileHelper {

    val FILENAME = "listinfo.dat"

    fun writeData (item: ArrayList<String>, context: Context)
    {
        var fos: FileOutputStream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE) //create file
        var oas = ObjectOutputStream(fos) //open file
        oas.writeObject(item) //write in file
        oas.close() //close file
    }

    fun readData(context: Context) : ArrayList<String>
    {
        var itemList: ArrayList<String>
        try {
            var fis: FileInputStream = context.openFileInput(FILENAME)
            var ois = ObjectInputStream(fis) //open file
            itemList = ois.readObject() as ArrayList<String> //read data
        }catch (e: FileNotFoundException){
            itemList = ArrayList()
        }

        return itemList
    }

}