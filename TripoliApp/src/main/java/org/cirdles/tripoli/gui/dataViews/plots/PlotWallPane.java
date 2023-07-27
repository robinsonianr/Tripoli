/*
 * Copyright 2022 James Bowring, Noah McLean, Scott Burdick, and CIRDLES.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cirdles.tripoli.gui.dataViews.plots;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.cirdles.tripoli.species.SpeciesRecordInterface;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author James F. Bowring
 */
public class PlotWallPane extends Pane {

    public static final double gridCellDim = 2.0;
    public static final double toolBarHeight = 35.0;
    public static double menuOffset = 30.0;
    private String iD;

    private boolean[] speciesChecked = new boolean[0];
    private boolean showFaradays = true;
    private boolean showPMs = true;
    private boolean showModels = true;

    private PlotWallPane(String iD) {
        this.iD = iD;
    }

    public static PlotWallPane createPlotWallPane(String iD) {
        if (iD == null) {
            return new PlotWallPane("NONE");
        } else {
            return new PlotWallPane(iD);
        }
    }

    public void tilePlots() {
        List<Node> plotPanes = getChildren()
                .stream()
                .filter(plot -> plot instanceof TripoliPlotPane)
                .collect(Collectors.toList());

        double widthTileCount = Math.min(5, plotPanes.size());
        double heightTileCount = Math.ceil(plotPanes.size() / widthTileCount);

        double displayWidth = ((getParent().getBoundsInParent().getWidth() - gridCellDim * 2.0) / widthTileCount);
        double tileWidth = displayWidth - displayWidth % gridCellDim;

        double displayHeight = ((getParent().getBoundsInParent().getHeight() - toolBarHeight - gridCellDim * heightTileCount) / heightTileCount);
        double tileHeight = displayHeight - displayHeight % gridCellDim;

        int plotIndex = 0;
        for (Node plot : plotPanes) {
            plot.setLayoutY(gridCellDim + toolBarHeight + tileHeight * Math.floor(plotIndex / widthTileCount));
            ((Pane) plot).setPrefHeight(tileHeight);
            plot.setLayoutX(gridCellDim + tileWidth * (plotIndex % widthTileCount));
            ((Pane) plot).setPrefWidth(tileWidth);

            ((TripoliPlotPane) plot).snapToGrid();

            plotIndex++;
        }
    }

    public void stackPlots() {
        double tileWidth;
        double displayHeight;
        if (iD.compareToIgnoreCase("OGTripoliSession") == 0) {
            tileWidth = ((AnchorPane) getParent()).getPrefWidth() - gridCellDim * 2.0;
            displayHeight = (((AnchorPane) getParent()).getPrefHeight() - toolBarHeight) / getCountOfPlots();
        } else {
            tileWidth = (getParent().getBoundsInParent().getWidth() - gridCellDim * 2.0);
            displayHeight = (getParent().getBoundsInParent().getHeight() - toolBarHeight) / getCountOfPlots();
        }

        double tileHeight = displayHeight - displayHeight % gridCellDim;

        int plotIndex = 0;
        for (Node plotPane : getChildren()) {
            if (plotPane instanceof TripoliPlotPane) {
                plotPane.setLayoutY(gridCellDim + toolBarHeight + tileHeight * plotIndex);
                ((Pane) plotPane).setPrefHeight(tileHeight);
                plotPane.setLayoutX(gridCellDim);
                ((Pane) plotPane).setPrefWidth(tileWidth);

                ((TripoliPlotPane) plotPane).snapToGrid();

                plotIndex++;
            }
        }
    }

    public void cascadePlots() {

        double cascadeLap = 20.0;
        double displayWidth = getParent().getBoundsInParent().getWidth() - cascadeLap * (getCountOfPlots() - 1);
        double tileWidth = displayWidth - gridCellDim;

        double displayHeight = getParent().getBoundsInParent().getHeight() - toolBarHeight - cascadeLap * (getCountOfPlots() - 1);
        double tileHeight = displayHeight - gridCellDim;

        int plotIndex = 0;
        for (Node plotPane : getChildren()) {
            if (plotPane instanceof TripoliPlotPane) {
                plotPane.setLayoutY(gridCellDim + toolBarHeight + cascadeLap * plotIndex);
                ((Pane) plotPane).setPrefHeight(tileHeight);
                plotPane.setLayoutX(gridCellDim + cascadeLap * plotIndex);
                ((Pane) plotPane).setPrefWidth(tileWidth);

                ((TripoliPlotPane) plotPane).snapToGrid();

                plotIndex++;
            }
        }
    }

    public void restoreAllPlots() {
        for (Node plotPane : getChildren()) {
            if (plotPane instanceof TripoliPlotPane) {
                ((TripoliPlotPane) plotPane).restorePlot();
            }
        }
    }

    public void toggleShowStatsAllPlots() {
        for (Node plotPane : getChildren()) {
            if (plotPane instanceof TripoliPlotPane) {
                ((TripoliPlotPane) plotPane).toggleShowStats();
            }
        }
    }

    public void toggleRatiosLogRatios() {
        for (Node plotPane : getChildren()) {
            if (plotPane instanceof TripoliPlotPane) {
                ((TripoliPlotPane) plotPane).toggleRatiosLogRatios();
            }
        }
    }

    private int getCountOfPlots() {
        List<Node> plots = getChildren()
                .stream()
                .filter(plot -> plot instanceof TripoliPlotPane)
                .collect(Collectors.toList());
        return plots.size();
    }

    public void buildToolBar() {
        ToolBar toolBar = new ToolBar();
        toolBar.setPrefHeight(toolBarHeight);

        Button button0 = new Button("Restore Plots");
        button0.setOnAction(event -> restoreAllPlots());

        Button button1 = new Button("Tile Plots");
        button1.setOnAction(event -> tilePlots());

        Button button2 = new Button("Stack Plots");
        button2.setOnAction(event -> stackPlots());

        Button button3 = new Button("Cascade Plots");
        button3.setOnAction(event -> cascadePlots());

        Button button4 = new Button("Toggle Plots' Stats");
        button4.setOnAction(event -> toggleShowStatsAllPlots());

        Button button5 = new Button("Toggle Ratios / LogRatios");
        button5.setOnAction(event -> toggleRatiosLogRatios());

        toolBar.getItems().addAll(button0, button5, button4, button1, button2, button3);
        getChildren().addAll(toolBar);
    }

    public void buildOGTripoliToolBar(List<SpeciesRecordInterface> species) {
        ToolBar toolBar = new ToolBar();
        toolBar.setPrefHeight(toolBarHeight);
        speciesChecked = new boolean[species.size()];

        CheckBox[] speciesCheckBoxes = new CheckBox[species.size()];
        for (int speciesIndex = 0; speciesIndex < species.size(); speciesIndex++) {
            speciesCheckBoxes[speciesIndex] = new CheckBox(species.get(speciesIndex).prettyPrintShortForm());
            speciesCheckBoxes[speciesIndex].setPrefWidth(75);
            toolBar.getItems().add(speciesCheckBoxes[speciesIndex]);
            int finalSpeciesIndex = speciesIndex;
            speciesCheckBoxes[speciesIndex].selectedProperty().addListener(
                    (ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                        speciesChecked[finalSpeciesIndex] = newVal;
                        rebuildPlot();
                    });
        }
        speciesCheckBoxes[0].setSelected(true);

        CheckBox showFaraday = new CheckBox("F");
        showFaraday.setSelected(true);
        showFaraday.setPrefWidth(50);
        toolBar.getItems().add(showFaraday);
        showFaraday.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    showFaradays = newVal;
                    rebuildPlot();
                });

        CheckBox showPM = new CheckBox("PM");
        showPM.setSelected(true);
        showPM.setPrefWidth(60);
        toolBar.getItems().add(showPM);
        showPM.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    showPMs = newVal;
                    rebuildPlot();
                });

        CheckBox showModel = new CheckBox("Model");
        showModel.setSelected(true);
        toolBar.getItems().add(showModel);
        showModel.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    showModels = newVal;
                    rebuildPlot();
                });

        getChildren().addAll(toolBar);
    }

    private void rebuildPlot() {
        for (Node plotPane : getChildren()) {
            if (plotPane instanceof TripoliPlotPane) {
                ((TripoliPlotPane) plotPane).updateSpeciesPlotted(speciesChecked, showFaradays, showPMs, showModels);
            }
        }
    }
}