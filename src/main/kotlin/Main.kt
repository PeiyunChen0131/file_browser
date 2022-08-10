import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString


class BorderDemo : Application() {
    private var homeDir = "${System.getProperty("user.dir")}/test/"
    private var showHidden = false
    fun getTreeViewComponent(root: TreeItem<Any?>, curPath: String) {
        val curFile = File(curPath)
        if (curFile.name.startsWith(".")) {
            if (!showHidden) {
                return
            }
        }
        val folderIcon: Node = ImageView(Image(javaClass.getResourceAsStream("folder.png")))
        if (curFile.isDirectory) {
            val curRoot = TreeItem<Any?>(curFile.name, folderIcon)
            root.children.add(curRoot)
            val fileLists = curFile.listFiles()
            Arrays.sort(fileLists)
            for (f in fileLists) {
                getTreeViewComponent(curRoot, f.path)
            }
        } else {
            val curNode = TreeItem<Any?>(curFile.name)
            root.children.add(curNode)
        }
    }

    private fun isImage(file: File): Boolean {
        return file.path.endsWith("jpg") || file.path.endsWith("png") || file.path.endsWith("bmp")
    }

    private fun isText(file: File): Boolean {
        return file.path.endsWith("txt") || file.path.endsWith("md")
    }

    private fun getPath(curNode: TreeItem<Any?>): String {
        var tmp = curNode.value.toString()
        var tmpPointer = curNode.parent
        while (tmpPointer.parent != null) {
            tmp = tmpPointer.value.toString() + File.separator + tmp
            tmpPointer = tmpPointer.parent
        }
        return Paths.get("").absolutePathString().toString() + File.separator + tmp
    }

    private fun getPathWithoutName(curNode: TreeItem<Any?>): String {
        var tmp = ""
        var tmpPointer = curNode.parent
        while (tmpPointer.parent != null) {
            tmp = tmpPointer.value.toString() + File.separator + tmp
            tmpPointer = tmpPointer.parent
        }
        return Paths.get("").absolutePathString().toString() + File.separator + tmp
    }


    override fun start(stage: Stage) {

        val topContainer = VBox()

        // top section: MenuBar
        var fileExpand = MenuItem("Expand")
        var fileCollapse = MenuItem("Collapse")

        var actionRename = MenuItem("Rename")
        var actionMove = MenuItem("Move")
        var actionDelete = MenuItem("Delete")

        var optionShowHidden = MenuItem("Show Hidden Files")


        val menuBar = MenuBar(
            Menu("File", null, fileExpand, fileCollapse),
            Menu("View"),
            Menu("Actions", null, actionRename, actionMove, actionDelete),
            Menu("Options", null, optionShowHidden)
        )

        var homeButton = Button("Home")
        var nextButton = Button("Next")
        var prevButton = Button("Prev")
        var deleteButton = Button("Delete")
        var moveButton = Button("Move")
        var renameButton = Button("Rename")
        // top section: toolbar
        val toolBar = ToolBar(
            homeButton,
            prevButton,
            nextButton,
            deleteButton,
            renameButton,
            moveButton
        )

        topContainer.children.add(menuBar)
        topContainer.children.add(toolBar)
        // side: tree view
        val folderIcon: Node = ImageView(Image(javaClass.getResourceAsStream("folder.png")))
        var rootItem = TreeItem<Any?>(homeDir, folderIcon)
        getTreeViewComponent(rootItem, homeDir)
        rootItem = rootItem.children[0]
        rootItem.isExpanded = false

        var tree = TreeView<Any?>(rootItem)
        tree.selectionModel.select(0)

        // center: text entry area
        var text = TextArea()
        text.isWrapText = true
        text.text = ""

        var image = ImageView()
        var center = HBox(text)

        // bottom: status bar
        var label = Label("No Item is selected.")
        val checkmark: Node = ImageView(Image(javaClass.getResourceAsStream("check.png")))
        val status = HBox(checkmark, label)

        // setup the scene
        val border = BorderPane()
        border.top = topContainer
        border.left = tree
        border.center = center
        border.bottom = status
        val scene = Scene(border)

        tree.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            var selected = tree.selectionModel.selectedItem
            if (selected != null) {
                val fullPath = getPathWithoutName(tree.selectionModel.selectedItem)
                val curFile = File(fullPath + tree.selectionModel.selectedItem.value.toString())
                if (!curFile.canRead()) {
                    val text = TextArea()
                    text.text = "File cannot be read"
                    text.isWrapText = true
                    center.children.clear()
                    center.children.add(text)
                    HBox.setHgrow(text, Priority.ALWAYS)
                    border.center = center
                }
                else if (curFile.isFile) {
                    if (isImage(curFile)) {
                        val stream = FileInputStream(fullPath + tree.selectionModel.selectedItem.value.toString())
                        val image = ImageView(Image(stream))
                        image.fitWidthProperty().bind(center.widthProperty())
                        image.fitHeightProperty().bind(center.heightProperty())
                        image.isPreserveRatio = true
                        center.children.clear()
                        center.children.add(image)
                        HBox.setHgrow(image, Priority.ALWAYS)
                        border.center = center
                    } else if (isText(curFile)){
                        val curPath = fullPath + tree.selectionModel.selectedItem.value.toString()
                        val text = TextArea()
                        text.isWrapText = true
                        val bytes = Files.readAllBytes(Paths.get(curPath))
                        val fileContent = String(bytes)
                        text.text = fileContent
                        center.children.clear()
                        center.children.add(text)
                        HBox.setHgrow(text, Priority.ALWAYS)
                        border.center = center
                    } else {
                        val text = TextArea()
                        text.isWrapText = true
                        text.text = "Unsupported type"
                        center.children.clear()
                        center.children.add(text)
                        HBox.setHgrow(text, Priority.ALWAYS)
                        border.center = center
                    }
                } else if (curFile.isDirectory) {
                    border.center = TextArea("")
                }
            }
        }

        tree.onKeyPressed = EventHandler { e: KeyEvent ->
            var selected = tree.selectionModel.selectedItems
            if (selected != null && e.code == KeyCode.ENTER) {
                selected[0].isExpanded = true
            }
            if (selected != null && (e.code == KeyCode.BACK_SPACE || e.code == KeyCode.DELETE)) {
                selected[0].isExpanded = false
            }

        }

        tree.onMouseClicked = EventHandler { e: MouseEvent ->
            var selected = tree.selectionModel.selectedItems
            if (selected != null && e.isPrimaryButtonDown && e.clickCount == 1) {
                selected[0].isExpanded = !selected[0].isExpanded
            }
            if (selected != null) {
                label.text = "Paths: " + getPath(tree.selectionModel.selectedItem) +
                        " \t\tName: " + selected[0].value.toString()
            }
        }

        homeButton.onAction = EventHandler<ActionEvent> {
            tree.selectionModel.select(0)
            tree.selectionModel.selectedItems[0].isExpanded = false
        }

        var prevIndex = 0
        nextButton.onAction = EventHandler<ActionEvent> {
            if (tree.selectionModel.selectedItems != null) {
                tree.selectionModel.selectedItems[0].isExpanded = true
                prevIndex = tree.selectionModel.selectedIndex
                tree.selectionModel.selectNext()

                if (tree.selectionModel.selectedIndex == prevIndex) {
                    tree.selectionModel.select(prevIndex)
                }
            }
        }

        prevButton.onAction = EventHandler {
            if (tree.selectionModel.selectedItems != null) {
                tree.selectionModel.selectedItems[0].isExpanded = false
                tree.selectionModel.selectPrevious()
            }
            if (tree.selectionModel.selectedIndex == 0) {
                tree.selectionModel.select(0)
            }
        }

        fileExpand.onAction = EventHandler {
            tree.selectionModel.selectedItem.isExpanded = true
        }
        fileCollapse.onAction = EventHandler {
            tree.selectionModel.selectedItem.isExpanded = false
        }


        val renameEvent = EventHandler<javafx.event.ActionEvent> {
            val dialog = TextInputDialog("")
            dialog.title = "Rename"
            dialog.headerText = "Enter the name that you want to change to"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val renamedName = result.get()
                val fullPath = getPathWithoutName(tree.selectionModel.selectedItem)
                try {
                    val curFile = File(fullPath + tree.selectionModel.selectedItem.value.toString())
                    val renamedFile = File(fullPath + renamedName)
                    val renameResult = curFile.renameTo(renamedFile)
                    if (renameResult) {
                        tree.selectionModel.selectedItem.value = renamedName
                    } else {
                        val alert = Alert(AlertType.ERROR)
                        alert.title = "Warning"
                        alert.contentText = "You input an invalid file name. Move Operation is cancelled."
                        alert.showAndWait()
                    }
                } catch (ex : Throwable) {
                    val alert = Alert(AlertType.ERROR)
                    alert.title = "Warning"
                    alert.contentText = "You input an invalid file name. Rename Operation is cancelled."
                    alert.showAndWait()
                }
            }
        }

        renameButton.onAction = renameEvent
        actionRename.onAction = renameEvent

        val moveEvent = EventHandler<javafx.event.ActionEvent> {
            val dialog = TextInputDialog("")
            dialog.title = "Move"
            dialog.headerText = "Enter the path that you want to move the file to, the path should be relative to home"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val targetPath = result.get()
                val fullPath = getPathWithoutName(tree.selectionModel.selectedItem)
                try {
                    val curFile = File(fullPath + tree.selectionModel.selectedItem.value.toString())
                    val renamedFile = File(homeDir + targetPath)
                    val moveResult = curFile.renameTo(renamedFile)
                    if (moveResult) {
                        rootItem = TreeItem<Any?>(homeDir, folderIcon)
                        getTreeViewComponent(rootItem, homeDir)
                        rootItem = rootItem.children[0]
                        rootItem.isExpanded = true
                        tree.root = rootItem
                        border.left = tree
                    } else {
                        val alert = Alert(AlertType.ERROR)
                        alert.title = "Warning"
                        alert.contentText = "You input an invalid file name. Move Operation is cancelled."
                        alert.showAndWait()
                    }

                } catch (ex : Throwable) {
                    val alert = Alert(AlertType.ERROR)
                    alert.title = "Warning"
                    alert.contentText = "You input an invalid file name. Move Operation is cancelled."
                    alert.showAndWait()
                }
            }
        }

        moveButton.onAction = moveEvent
        actionMove.onAction = moveEvent

        val deleteEvent = EventHandler<javafx.event.ActionEvent> {
            // confirmation
            val confirmation = Alert(AlertType.CONFIRMATION)
            confirmation.title = "Confirmation dialog"
            confirmation.contentText = "Do you wish to proceed?"
            val result1 = confirmation.showAndWait()

            if (result1.isPresent) {
                when (result1.get()) {
                    ButtonType.OK -> {
                        val fullPath = getPathWithoutName(tree.selectionModel.selectedItem)
                        val curFile = File(fullPath + tree.selectionModel.selectedItem.value.toString())
                        curFile.delete()

                        rootItem = TreeItem<Any?>(homeDir, folderIcon)
                        getTreeViewComponent(rootItem, homeDir)
                        rootItem = rootItem.children[0]
                        rootItem.isExpanded = true
                        tree.root = rootItem
                        tree.selectionModel.select(0)
                        border.left = tree
                    }
                    ButtonType.CANCEL -> {

                    }
                }
            }
        }
        deleteButton.onAction = deleteEvent
        actionDelete.onAction = deleteEvent


        val showHiddenEvent = EventHandler<javafx.event.ActionEvent> {
            showHidden = !showHidden
            rootItem = TreeItem<Any?>(homeDir, folderIcon)
            getTreeViewComponent(rootItem, homeDir)
            rootItem = rootItem.children[0]
            rootItem.isExpanded = true
            tree.root = rootItem
            tree.selectionModel.select(0)
        }
        optionShowHidden.onAction = showHiddenEvent

        // setup and show the window
        stage.title = "File Browser"
        stage.isResizable = true


        stage.width = 640.0

        stage.height = 480.0

        stage.scene = scene
        stage.show()
    }
}

