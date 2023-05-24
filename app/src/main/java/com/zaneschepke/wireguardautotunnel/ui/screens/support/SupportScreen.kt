package com.zaneschepke.wireguardautotunnel.ui.screens.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun SupportScreen(padding : PaddingValues) {

    val context = LocalContext.current

    fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        context.startActivity(intent)
    }

        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
            Text(stringResource(R.string.support_text), textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp), fontSize = 15.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    openWebPage(context.resources.getString(R.string.discord_url))
                }) {
                    Icon(imageVector = ImageVector.vectorResource(R.drawable.discord), "Discord")
                }
                IconButton(onClick = {
                    openWebPage(context.resources.getString(R.string.github_url))
                }) {
                    Icon(imageVector = ImageVector.vectorResource(R.drawable.github), "Github")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(id = R.string.privacy_policy), style = TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable {
                openWebPage(context.resources.getString(R.string.privacy_policy_url))
            })
            Text("App version: ${com.zaneschepke.wireguardautotunnel.BuildConfig.VERSION_NAME}", Modifier.padding(25.dp))
        }
    }