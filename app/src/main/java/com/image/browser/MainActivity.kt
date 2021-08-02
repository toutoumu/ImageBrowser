package com.image.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.image.browser.databinding.ActivityMainBinding
import com.kongzue.dialog.v3.TipDialog
import com.kongzue.dialog.v3.TipDialog.TYPE.WARNING
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

class MainActivity : AppCompatActivity(), PermissionCallbacks {

    private lateinit var bind: ActivityMainBinding

    companion object {
        private const val REQUEST_CODE_PERMISSION = 111
        private const val REQUEST_CODE_LOGIN = 112
        private const val REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11 = 123
    }

    // 该应用所需权限
    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            // 管理全部文件的权限判断是否获取MANAGE_EXTERNAL_STORAGE权限：
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11)
            }
        }
        // 权限检查
        this.activation()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    /**
     * 权限检查
     */
    @SuppressLint("CheckResult")
    @AfterPermissionGranted(REQUEST_CODE_PERMISSION)
    fun activation() {
        if (EasyPermissions.hasPermissions(this, *this.permissions)) {
            this.toMainFragment()
            return
        }
        // 权限请求
        EasyPermissions.requestPermissions(this, "使用该应用需要以下权限", REQUEST_CODE_PERMISSION, *this.permissions)
    }

    /**
     * 权限被拒绝 如果这里请求的权限没有在清单文件中声明,这里会反复调用
     */
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        AppSettingsDialog.Builder(this)
            .setRationale("使用该应用需要权限")
            .setTitle("缺少权限")
            .build()
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11 && resultCode == RESULT_OK) {
            TipDialog.show(this, "已经授予权限！", WARNING)
            return
        }
        // 打开设置页面返回
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (EasyPermissions.hasPermissions(this, *this.permissions)) {
                this.activation()
            } else {
                finish() // 没有权限则关闭页面
            }
        }
        // 登录页面返回
        else if (requestCode == REQUEST_CODE_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                toMainFragment()// 如果已经登录
            } else {
                finish() // 如果未登录
            }
        }
    }

    /**
     * 跳转到首页
     */
    private fun toMainFragment() {
        val intent = Intent(this, PictureActivity::class.java)
        startActivity(intent)
        finish()
    }
}