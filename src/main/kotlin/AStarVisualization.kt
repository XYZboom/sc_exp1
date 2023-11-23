import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.math.pow
import kotlin.math.sqrt

class AStarVisualization : JFrame() {
    private val gridSize = 15
    private val gridPixelSize = 800
    private val cellPixelSize = gridPixelSize / gridSize
    private val grid: Array<Array<Node>> = Array(gridSize) { i ->
        Array(gridSize) { j ->
            Node(i, j)
        }
    }
    private val openSet: MutableList<Node> = mutableListOf()
    private val closedSet: MutableList<Node> = mutableListOf()
    private var startNode: Node? = grid[0][0]
    private var endNode: Node? = grid[gridSize - 1][gridSize - 1]
    private var path: MutableList<Node> = mutableListOf()
    private var isRunning: Boolean = false

    init {
        title = "A* Pathfinding Visualization"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(gridPixelSize, gridPixelSize)
        isVisible = true

        val controlPanel = JPanel()
        val startButton = JButton("Start")
        startButton.addActionListener(StartButtonListener())
        val stepButton = JButton("Step")
        stepButton.addActionListener(StepButtonListener())
        val resetButton = JButton("Reset")
        resetButton.addActionListener(ResetButtonListener())
        controlPanel.add(startButton)
        controlPanel.add(stepButton)
        controlPanel.add(resetButton)

        layout = BorderLayout()
        add(controlPanel, BorderLayout.SOUTH)
        val gridPanel = GridPanel()
        gridPanel.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (!isRunning) {
                    val x = e.x / (e.component.width / gridSize)
                    val y = e.y / (e.component.height / gridSize)
                    println("e.x: ${e.x}, x: $x")
                    println("e.y: ${e.y}, y: $y")
                    grid[x][y].let {
                        if (it !== startNode && it !== endNode)
                            it.isWall = !e.isControlDown
                    }
                    repaint()
                }
            }
        })
        add(gridPanel, BorderLayout.CENTER)
    }

    private fun reset() {
        openSet.clear()
        closedSet.clear()
        path.clear()

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                grid[i][j].reset()
            }
        }

        startNode = grid[0][0]
        endNode = grid[gridSize - 1][gridSize - 1]
        isRunning = false

        repaint()
    }

    private fun reconstructPath(current: Node) {
        path.clear()
        var node: Node? = current
        while (node != null) {
            path.add(node)
            node = node.cameFrom
        }
    }

    private fun heuristic(a: Node, b: Node): Double {
        return sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2))
    }

    private fun findNeighbors(node: Node): List<Node> {
        val neighbors = mutableListOf<Node>()

        val x = node.x
        val y = node.y

        if (x > 0) neighbors.add(grid[x - 1][y])
        if (x < gridSize - 1) neighbors.add(grid[x + 1][y])
        if (y > 0) neighbors.add(grid[x][y - 1])
        if (y < gridSize - 1) neighbors.add(grid[x][y + 1])

        return neighbors
    }

    private fun startAlgorithm() {
        if (startNode == null || endNode == null) return

        openSet.clear()
        closedSet.clear()
        path.clear()

        openSet.add(startNode!!)
        isRunning = true

        step()
    }

    private fun step() {
        if (openSet.isNotEmpty() && isRunning) {
            val currentNode = openSet.minByOrNull { it.fScore }
            if (currentNode == endNode) {
                reconstructPath(currentNode!!)
                isRunning = false
                repaint()
                return
            }

            openSet.remove(currentNode)
            closedSet.add(currentNode!!)

            val neighbors = findNeighbors(currentNode)
            for (neighbor in neighbors) {
                if (neighbor in closedSet || neighbor.isWall) continue

                val tentativeGScore = currentNode.gScore + 1
                val betterPath = tentativeGScore < neighbor.gScore

                if (betterPath || neighbor !in openSet) {
                    neighbor.gScore = tentativeGScore
                    neighbor.hScore = heuristic(neighbor, endNode!!)
                    neighbor.fScore = neighbor.gScore + neighbor.hScore
                    neighbor.cameFrom = currentNode

                    if (neighbor !in openSet) {
                        openSet.add(neighbor)
                    }
                }
            }
        }
        repaint()
    }

    private inner class GridPanel : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D

            val cellWidth = width / gridSize
            val cellHeight = height / gridSize

            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    val node = grid[i][j]

                    g2d.color = when {
                        node.isWall -> Color.BLACK
                        node in closedSet -> Color.RED
                        node in openSet -> Color.GREEN
                        else -> Color.WHITE
                    }
                    g2d.fillRect(i * cellWidth, j * cellHeight, cellWidth, cellHeight)

                    g2d.color = Color.BLACK
                    g2d.drawRect(i * cellWidth, j * cellHeight, cellWidth, cellHeight)
                    g2d.drawString("f: ${node.fScore}", i * cellWidth, (j + 1) * cellHeight - cellHeight / 3)
                    g2d.drawString("g: ${node.gScore}", i * cellWidth, (j + 1) * cellHeight - cellHeight / 3 * 2)
                    g2d.drawString("h: ${node.hScore}", i * cellWidth, (j + 1) * cellHeight)
                }

                for (node in path) {
                    g2d.color = Color.BLUE
                    g2d.fillRect(node.x * cellWidth, node.y * cellHeight, cellWidth, cellHeight)
                }

                if (startNode != null) {
                    g2d.color = Color.ORANGE
                    g2d.fillRect(startNode!!.x * cellWidth, startNode!!.y * cellHeight, cellWidth, cellHeight)
                }

                if (endNode != null) {
                    g2d.color = Color.CYAN
                    g2d.fillRect(endNode!!.x * cellWidth, endNode!!.y * cellHeight, cellWidth, cellHeight)
                }
            }
        }
    }

    private inner class StartButtonListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            startAlgorithm()
        }
    }

    private inner class StepButtonListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            step()
        }
    }

    private inner class ResetButtonListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            reset()
        }
    }

    private inner class Node(val x: Int, val y: Int) {
        var gScore: Double = 0.0
        var hScore: Double = 0.0
        var fScore: Double = 0.0
        var cameFrom: Node? = null
        var isWall: Boolean = false

        fun reset() {
            gScore = 0.0
            hScore = 0.0
            fScore = 0.0
            cameFrom = null
            isWall = false
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        AStarVisualization()
    }
}