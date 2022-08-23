package org.cirdles.tripoli.gui.dataViews.plots;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.cirdles.tripoli.visualizationUtilities.linePlots.MultiLinePlotBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class MultiLinePlotLogX extends AbstractDataView {

    private final MultiLinePlotBuilder multiLinePlotBuilder;
    private double[][] yData;
    /**
     * @param bounds
     * @param multiLinePlotBuilder
     */
    public MultiLinePlotLogX(Rectangle bounds, MultiLinePlotBuilder multiLinePlotBuilder) {
        super(bounds, 50, 5);
        this.multiLinePlotBuilder = multiLinePlotBuilder;
    }

    @Override
    public void preparePanel() {
        xAxisData = multiLinePlotBuilder.getxData();
//        yAxisData = multiLinePlotBuilder.getyData();
        yData = multiLinePlotBuilder.getyData();

        minX = Math.log(xAxisData[0]);
        maxX = Math.log(xAxisData[xAxisData.length - 1]);

        ticsX = TicGeneratorForAxes.generateTics(minX, maxX * 1.1, (int) (graphWidth / 100.0));
        double xMarginStretch = TicGeneratorForAxes.generateMarginAdjustment(minX, maxX, 0.01);
        minX -= xMarginStretch;
        maxX += xMarginStretch;

        minY = Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;

        for (int i = 0; i < yData.length; i++) {
            for (int j = 0; j < yData[i].length; j ++) {
                minY = StrictMath.min(minY, yData[i][j]);
                maxY = StrictMath.max(maxY, yData[i][j]);
            }
        }
        ticsY = TicGeneratorForAxes.generateTics(minY, maxY, (int) (graphHeight / 15.0));
        if ((ticsY != null) && (ticsY.length > 1)) {
            // force y to tics
            minY = ticsY[0].doubleValue();
            maxY = ticsY[ticsY.length - 1].doubleValue();
            // adjust margins
            double yMarginStretch = TicGeneratorForAxes.generateMarginAdjustment(minY, maxY, 0.1);
            minY -= yMarginStretch * 2.0;
            maxY += yMarginStretch;
        }

        setDisplayOffsetY(0.0);
        setDisplayOffsetX(0.0);

        this.repaint();
    }

    @Override
    public void paint(GraphicsContext g2d) {
        super.paint(g2d);

        Text text = new Text();
        text.setFont(Font.font("SansSerif", 12));
        int textWidth = 0;

        g2d.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        g2d.setFill(Paint.valueOf("BLUE"));
        g2d.fillText(multiLinePlotBuilder.getTitle(), leftMargin + 25, 15);

        g2d.setLineWidth(2.0);
        // new line plots
        // specify cool heatmap
        double rgbStartRed = 151.0 / 255.0;
        double rgbStartGreen = 248.0 / 255.0;
        double rgbStartBlue = 253.0 / 255.0;
        double rgbEndRed = 231.0 / 255.0;
        double rgbEndGreen = 56.0 / 255.0;
        double rgbEndBlue = 244.0 / 255.0;
        double redDelta = (rgbEndRed - rgbStartRed) / yData.length;
        double greenDelta = (rgbEndGreen - rgbStartGreen ) / yData.length;
        double blueDelta = (rgbEndBlue - rgbStartBlue) / yData.length;

        for( int y =0; y < yData.length; y++) {
            g2d.setStroke(Color.color(rgbStartRed + redDelta * y, rgbStartGreen + greenDelta * y, rgbStartBlue + blueDelta * y));
            g2d.beginPath();
            g2d.moveTo(mapX(Math.log(xAxisData[0])), mapY(yData[y][0]));
            for (int i = 0; i < xAxisData.length; i++) {
                // line tracing through points
                g2d.lineTo(mapX(Math.log(xAxisData[i])), mapY(yData[y][i]));
            }
            g2d.stroke();
        }

        if (ticsY.length > 1) {
            // border and fill
            g2d.setLineWidth(0.5);
            g2d.setStroke(Paint.valueOf("BLACK"));
            g2d.strokeRect(
                    mapX(minX),
                    mapY(ticsY[ticsY.length - 1].doubleValue()),
                    graphWidth,
                    StrictMath.abs(mapY(ticsY[ticsY.length - 1].doubleValue()) - mapY(ticsY[0].doubleValue())));

            g2d.setFill(Paint.valueOf("BLACK"));

            // ticsY
            float verticalTextShift = 3.2f;
            g2d.setFont(Font.font("SansSerif", 10));
            if (ticsY != null) {
                for (int i = 0; i < ticsY.length; i++) {
                    g2d.strokeLine(
                            mapX(minX), mapY(ticsY[i].doubleValue()), mapX(maxX), mapY(ticsY[i].doubleValue()));

                    // left side
                    text.setText(ticsY[i].toString());
                    textWidth = (int) text.getLayoutBounds().getWidth();
                    g2d.fillText(text.getText(),//
                            (float) mapX(minX) - textWidth - 5f,
                            (float) mapY(ticsY[i].doubleValue()) + verticalTextShift);

                }
                // ticsX
                if (ticsX != null) {
                    for (int i = 0; i < ticsX.length - 1; i++) {
                        try {
                            g2d.strokeLine(
                                    mapX(ticsX[i].doubleValue()),
                                    mapY(ticsY[0].doubleValue()),
                                    mapX(ticsX[i].doubleValue()),
                                    mapY(ticsY[0].doubleValue()) + 5);

                            // bottom
                            String xText = (new BigDecimal(Double.toString(Math.exp(ticsX[i].doubleValue())))).setScale(-1, RoundingMode.HALF_UP).toPlainString();
                            g2d.fillText(xText,
                                    (float) mapX(ticsX[i].doubleValue()) - 5f,
                                    (float) mapY(ticsY[0].doubleValue()) + 15);

                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }
}