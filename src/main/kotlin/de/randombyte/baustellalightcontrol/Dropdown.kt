package de.randombyte.baustellalightcontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

@Composable
fun Dropdown(
    modifier: Modifier,
    items: List<String>,
    defaultSelectedIndex: Int,
    onSelect: (item: String, index: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(defaultSelectedIndex) }

    var rowWidth by remember { mutableStateOf(0.dp) }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = { expanded = true })
                .border(2.dp, Color.Black, MaterialTheme.shapes.small)
                .padding(4.dp)
                .onGloballyPositioned { coordinates -> rowWidth = coordinates.size.width.dp }
        ) {
            Text(
                text = items.getOrNull(selectedIndex) ?: "",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(rowWidth)
        ) {
            items.forEachIndexed { index, text ->
                DropdownMenuItem(
                    modifier = Modifier.background(if (selectedIndex == index) Color.LightGray else MaterialTheme.colors.surface),
                    onClick = {
                        selectedIndex = index
                        expanded = false
                        onSelect(items[index], index)
                    }
                ) {
                    Text(text)
                }
            }
        }
    }
}
