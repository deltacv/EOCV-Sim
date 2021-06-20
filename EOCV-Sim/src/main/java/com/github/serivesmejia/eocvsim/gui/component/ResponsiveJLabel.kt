import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JLabel


class ResponsiveJLabel(text: String) : JLabel(text) {

    private val SIZE = 256
    private var image: BufferedImage? = null

    init {
        image = createImage(super.getText())
    }

    override fun setText(text: String?) {
        super.setText(text)
        image = createImage(super.getText())
        repaint()
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(image!!.width / 2, image!!.height / 2)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.drawImage(image, 0, 0, width, height, null)
    }

    private fun createImage(label: String): BufferedImage? {
        val font = Font(Font.SERIF, Font.PLAIN, SIZE)
        val frc = FontRenderContext(null, true, true)
        val layout = TextLayout(label, font, frc)
        val r = layout.getPixelBounds(null, 0f, 0f)
        println(r)
        val bi = BufferedImage(
            r.width + 1, r.height + 1, BufferedImage.TYPE_INT_RGB
        )
        val g2d = bi.graphics as Graphics2D
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.color = background
        g2d.fillRect(0, 0, bi.width, bi.height)
        g2d.color = foreground
        layout.draw(g2d, 0f, -r.y.toFloat())
        g2d.dispose()
        return bi
    }

}