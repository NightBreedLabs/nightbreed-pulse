package com.nightbreedlabs.pulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.model.DiagnosticTask
import com.nightbreedlabs.pulse.model.TaskStatus
import com.nightbreedlabs.pulse.ui.theme.*

@Composable
fun TaskCard(
    task: DiagnosticTask,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = TokyoSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoTextPrimary
                    )
                }

                when (task.status) {
                    TaskStatus.IDLE -> {
                        IconButton(
                            onClick = onExecute,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = TokyoPrimaryContainer,
                                contentColor = TokyoPrimary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Run")
                        }
                    }
                    TaskStatus.RUNNING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TokyoPrimary,
                            strokeWidth = 2.5.dp
                        )
                    }
                    TaskStatus.SUCCESS -> {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = TokyoSuccess,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    TaskStatus.FAILED -> {
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = "Failed",
                            tint = TokyoError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.description,
                fontSize = 12.sp,
                color = TokyoTextSecondary,
                lineHeight = 16.sp
            )

            if (task.outputLog.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TokyoSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "OUTPUT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoTextMuted
                            )
                            if (task.executionTimeMs > 0) {
                                Text(
                                    text = "${task.executionTimeMs}ms",
                                    fontSize = 10.sp,
                                    color = TokyoTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.outputLog,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TokyoTextPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
