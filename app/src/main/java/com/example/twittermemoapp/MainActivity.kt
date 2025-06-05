package com.example.twittermemoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.html.HtmlPlugin
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        private var instance: MainActivity? = null
        fun getInstance(): MainActivity? = instance
    }

    private val allMemos = mutableListOf<Memo>() // 全てのメモ（削除済みも含む）
    private val activeMemos = mutableListOf<Memo>() // アクティブなメモのみ
    private lateinit var adapter: MemoAdapter
    private lateinit var markwon: Markwon
    private lateinit var editor: MarkwonEditor
    private lateinit var appPreferences: AppPreferences

    // ファイル操作用のリクエストコード
    private val CREATE_FILE = 1
    private val OPEN_FILE = 2
    private val AUTO_SAVE_FILE = "auto_save.md"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        instance = this
        appPreferences = AppPreferences(this)

        // 自動保存データの読み込み
        loadAutoSave()

        // Markwonの初期化
        markwon = Markwon.builder(this)
            .usePlugin(HtmlPlugin.create())
            .build()

        editor = MarkwonEditor.create(markwon)

        // RecyclerViewの設定
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = MemoAdapter(
            activeMemos,
            markwon,
            onMemoClick = { memo -> showMemoDialog(memo) },
            onMemoLongClick = { memo -> onMultiSelectStarted() },
            onSelectionChanged = { memo, isSelected -> onSelectionChanged() }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // メニューの設定
        setupMenu()

        // FABのクリックリスナー
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showMemoDialog()
        }

        // デフォルトメモの自動開封
        openDefaultMemoIfExists()
        
        // 期限切れメモの自動削除
        clearExpiredMemos()
    }

    private fun onMultiSelectStarted() {
        // ツールバーを更新してマルチセレクトモードを表示
        invalidateOptionsMenu()
    }

    private fun onSelectionChanged() {
        // ツールバーのタイトルを更新
        if (adapter.isInMultiSelectMode()) {
            supportActionBar?.title = "${adapter.getSelectedCount()}個選択中"
        } else {
            supportActionBar?.title = "メモ"
        }
        invalidateOptionsMenu()
    }

    private fun deleteSelectedMemos() {
        val selectedMemos = adapter.getSelectedMemos()
        if (selectedMemos.isEmpty()) return

        // デフォルトメモが含まれているかチェック
        val defaultMemoInSelection = selectedMemos.any { it.id == appPreferences.defaultMemoId }
        if (defaultMemoInSelection) {
            Toast.makeText(this, "デフォルトメモは削除できません", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("メモの削除")
            .setMessage("選択したメモを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                selectedMemos.forEach { memo ->
                    softDeleteMemo(memo)
                }
                adapter.exitMultiSelectMode()
                refreshActiveMemos()
                Toast.makeText(this, "${selectedMemos.size}個のメモを削除しました", Toast.LENGTH_SHORT).show()
                autoSave()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun softDeleteMemo(memo: Memo) {
        val index = allMemos.indexOfFirst { it.id == memo.id }
        if (index != -1) {
            allMemos[index] = memo.markAsDeleted()
        }
    }

    private fun showMemoDialog(memo: Memo? = null) {
        val dialog = MemoDialogFragment.newInstance(editor, markwon, memo)
        dialog.show(supportFragmentManager, "memo_dialog")
    }

    fun addMemo(content: String) {
        val newMemo = Memo(content = content)
        allMemos.add(0, newMemo)
        refreshActiveMemos()
        autoSave()
        
        // 最初のメモの場合、自動的にデフォルトメモに設定
        if (allMemos.filter { !it.isDeleted }.size == 1) {
            appPreferences.defaultMemoId = newMemo.id
            adapter.notifyDataSetChanged()
        }
    }

    fun updateMemo(memo: Memo, newContent: String) {
        val index = allMemos.indexOfFirst { it.id == memo.id }
        if (index != -1) {
            // タイムスタンプを更新
            allMemos[index] = Memo(id = memo.id, content = newContent)
            refreshActiveMemos()
            autoSave()
        }
    }

    private fun refreshActiveMemos() {
        activeMemos.clear()
        activeMemos.addAll(allMemos.filter { !it.isDeleted })
        adapter.notifyDataSetChanged()
    }

    private fun setupMenu() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // タイトルを設定
        supportActionBar?.apply {
            title = "メモ"
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (adapter.isInMultiSelectMode()) {
            menuInflater.inflate(R.menu.multiselect_menu, menu)
        } else {
            menuInflater.inflate(R.menu.main_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                createFile()
                true
            }
            R.id.action_open -> {
                openFile()
                true
            }
            R.id.action_trash -> {
                startActivity(Intent(this, TrashActivity::class.java))
                true
            }
            R.id.action_settings -> {
                showDefaultMemoDialog()
                true
            }
            R.id.action_delete_selected -> {
                deleteSelectedMemos()
                true
            }
            R.id.action_cancel_selection -> {
                adapter.exitMultiSelectMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
            putExtra(Intent.EXTRA_TITLE, "memo.md")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
        }
        startActivityForResult(intent, OPEN_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }

        data?.data?.also { uri ->
            when (requestCode) {
                CREATE_FILE -> writeFile(uri)
                OPEN_FILE -> readFile(uri)
            }
        }
    }

    private fun writeFile(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val content = StringBuilder()
                activeMemos.forEach { memo ->
                    content.append(memo.getContentWithTimestamp())
                    content.append("\n\n")
                }
                outputStream.write(content.toString().toByteArray())
            }
            Toast.makeText(this, "ファイルを保存しました", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                // 既存のメモをクリア
                allMemos.clear()

                // マークダウンファイルを解析してメモに変換
                val sections = content.split("\n\n")
                sections.forEach { section ->
                    if (section.isNotEmpty()) {
                        // タイムスタンプが含まれている場合は抽出
                        val lines = section.trim().lines()
                        if (lines.isNotEmpty()) {
                            // [timestamp] 形式のタイムスタンプを検索
                            val lastLine = lines.lastOrNull() ?: ""
                            val timestampRegex = "\\[([^\\]]+)\\]".toRegex()
                            val timestampMatch = timestampRegex.find(lastLine)
                            
                            if (timestampMatch != null) {
                                val timestamp = timestampMatch.groupValues[1]
                                val contentLines = if (lastLine.trim() == timestampMatch.value) {
                                    lines.dropLast(1)
                                } else {
                                    lines.map { it.replace(timestampRegex, "").trim() }
                                }
                                val memoContent = contentLines.joinToString("\n").trim()
                                allMemos.add(Memo(content = memoContent, timestamp = timestamp))
                            } else {
                                // タイムスタンプがない場合はそのまま追加
                                allMemos.add(Memo(content = section.trim()))
                            }
                        }
                    }
                }
                refreshActiveMemos()
            }
            Toast.makeText(this, "ファイルを読み込みました", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 自動保存関連のメソッド
    private fun autoSave() {
        try {
            val file = File(filesDir, AUTO_SAVE_FILE)
            file.writeText(buildString {
                allMemos.forEach { memo ->
                    append("MEMO_START\n")
                    append("${memo.content}\n")
                    append("[${memo.timestamp}]\n")
                    append("DELETED:${memo.isDeleted}\n")
                    memo.deletedAt?.let { append("DELETED_AT:$it\n") }
                    append("MEMO_END\n\n")
                }
            })
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadAutoSave() {
        try {
            val file = File(filesDir, AUTO_SAVE_FILE)
            if (file.exists()) {
                val content = file.readText()
                val sections = content.split("MEMO_START")
                allMemos.clear()
                
                sections.forEach { section ->
                    if (section.contains("MEMO_END")) {
                        val lines = section.trim().lines()
                        val endIndex = lines.indexOfFirst { it.trim() == "MEMO_END" }
                        if (endIndex > 0) {
                            val memoLines = lines.subList(0, endIndex)
                            
                            // メタデータの抽出
                            var isDeleted = false
                            var deletedAt: Long? = null
                            var timestamp = ""
                            val contentLines = mutableListOf<String>()
                            
                            memoLines.forEach { line ->
                                when {
                                    line.startsWith("DELETED:") -> isDeleted = line.substringAfter(":").toBoolean()
                                    line.startsWith("DELETED_AT:") -> deletedAt = line.substringAfter(":").toLongOrNull()
                                    line.matches("\\[([^\\]]+)\\]".toRegex()) -> timestamp = line.removeSurrounding("[", "]")
                                    !line.startsWith("DELETED") && line.isNotEmpty() -> contentLines.add(line)
                                }
                            }
                            
                            val memoContent = contentLines.joinToString("\n").trim()
                            if (memoContent.isNotEmpty() || timestamp.isNotEmpty()) {
                                allMemos.add(Memo(
                                    content = memoContent,
                                    timestamp = timestamp.ifEmpty { 
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()) 
                                    },
                                    isDeleted = isDeleted,
                                    deletedAt = deletedAt
                                ))
                            }
                        }
                    }
                }
                refreshActiveMemos()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // アプリがバックグラウンドに行く時に自動保存
    override fun onPause() {
        super.onPause()
        autoSave()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // デフォルトメモ関連のメソッド
    private fun openDefaultMemoIfExists() {
        if (appPreferences.hasDefaultMemo()) {
            val defaultMemo = activeMemos.find { it.id == appPreferences.defaultMemoId }
            defaultMemo?.let { showMemoDialog(it) }
        }
    }

    private fun showDefaultMemoDialog() {
        val memoTitles = activeMemos.map { 
            "${it.content.take(30)}${if (it.content.length > 30) "..." else ""}"
        }.toTypedArray()
        
        val currentDefault = activeMemos.indexOfFirst { it.id == appPreferences.defaultMemoId }
        
        AlertDialog.Builder(this)
            .setTitle("デフォルトメモの設定")
            .setSingleChoiceItems(memoTitles, currentDefault) { dialog, which ->
                appPreferences.defaultMemoId = activeMemos[which].id
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "デフォルトメモを設定しました", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // ソフト削除関連のメソッド
    fun getDeletedMemos(): List<Memo> {
        return allMemos.filter { it.isDeleted }
    }

    fun restoreMemo(memo: Memo) {
        val index = allMemos.indexOfFirst { it.id == memo.id }
        if (index != -1) {
            allMemos[index] = memo.restore()
            refreshActiveMemos()
            autoSave()
        }
    }

    fun permanentlyDeleteMemo(memo: Memo) {
        allMemos.removeAll { it.id == memo.id }
        autoSave()
    }

    fun clearExpiredMemos() {
        val expiredMemos = allMemos.filter { it.isDeleted && it.canPermanentlyDelete() }
        allMemos.removeAll { it.isDeleted && it.canPermanentlyDelete() }
        if (expiredMemos.isNotEmpty()) {
            autoSave()
        }
    }

    override fun onBackPressed() {
        if (adapter.isInMultiSelectMode()) {
            adapter.exitMultiSelectMode()
        } else {
            super.onBackPressed()
        }
    }
}