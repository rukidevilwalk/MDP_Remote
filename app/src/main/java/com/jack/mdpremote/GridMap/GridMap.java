package com.jack.mdpremote.GridMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jack.mdpremote.MainActivity;
import com.jack.mdpremote.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class GridMap extends View {

    private static final int COLUMN = 15, ROW = 20;

    private static float cellsSize;

    private static JSONObject receivedPayload = new JSONObject();

    private static JSONObject mapInfo;

    private static JSONObject manualMapInfo;

    private static Cell[][] cells;

    private static String robotFacing = "None";

    private static int[] startCoordinates = new int[]{-1, -1};

    private static int[] currentCoordinates = new int[]{-1, -1};

    private static int[] previousCoordinates = new int[]{-1, -1};

    private static int[] wpCoordinates = new int[]{-1, -1};

    private static ArrayList<String[]> imageCoordinates = new ArrayList<>();

    private static ArrayList<int[]> obstacleCoordinates = new ArrayList<>();

    private static boolean autoUpdate = false;

    private static boolean mapRendered = false;

    private static boolean robotRenderable = false;

    private static boolean setWPStatus = false;

    private static boolean startCoordinatesStatus = false;

    private static boolean setObstacleStatus = false;

    private static boolean unexploredCellStatus = false;

    private static boolean setExploredStatus = false;

    private static boolean validPosition = false;

    private Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_error);

    private Paint blackColor = new Paint();

    private Paint whiteColor = new Paint();

    private Paint obstacleColor = new Paint();

    private Paint robotColor = new Paint();

    private Paint endColor = new Paint();

    private Paint startColor = new Paint();

    private Paint wpColor = new Paint();

    private Paint unexploredColor = new Paint();

    private Paint exploredColor = new Paint();

    private Paint imageColor = new Paint();

    private Paint fastestPathColor = new Paint();

    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;


    public GridMap(Context context) {

        super(context);

        init(null);
    }


    public GridMap(Context context, @Nullable AttributeSet attrs) {

        super(context, attrs);

        init(attrs);

        whiteColor.setColor(Color.WHITE);
        unexploredColor.setColor(Color.GRAY);
        robotColor.setColor(Color.CYAN);

        endColor.setColor(Color.GREEN);

        exploredColor.setColor(Color.WHITE);

        imageColor.setColor(Color.BLACK);

        fastestPathColor.setColor(Color.MAGENTA);
        blackColor.setStyle(Paint.Style.FILL_AND_STROKE);

        obstacleColor.setColor(Color.BLACK);


        startColor.setColor(Color.CYAN);

        wpColor.setColor(Color.YELLOW);


    }


    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    private int convertRow(int row) {
        return (20 - row);
    }


    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        ArrayList<String[]> imageCoordinates = this.getImageCoordinates();

        int[] currentCoordinates = this.getCurrentCoordinates();

        if (!this.getMapDrawn()) {

            canvas.drawColor(Color.parseColor("#000000"));

            String[] placeholderImageCoord = new String[3];

            placeholderImageCoord[0] = "999";

            placeholderImageCoord[1] = "999";

            placeholderImageCoord[2] = "placeholder";

            imageCoordinates.add(placeholderImageCoord);

            this.createCell();

            this.setEndCoordinates(14, 19);

            mapRendered = true;
        }


        this.renderCell(canvas);

        this.renderAxisLabels(canvas);

        if (this.getCanDrawRobot())

            this.renderRobot(canvas, currentCoordinates);

        this.renderImages(canvas, imageCoordinates);

    }

    private void createCell() {

        cells = new Cell[COLUMN + 1][ROW + 1];

        this.calculateDimension();

        cellsSize = this.getCellSize();

        for (int x = 0; x <= COLUMN; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellsSize + (cellsSize / 30), y * cellsSize + (cellsSize / 30), (x + 1) * cellsSize, (y + 1) * cellsSize, unexploredColor, "unexplored");
    }


    public void setAutoUpdate(boolean autoUpdate) throws JSONException {

        if (!autoUpdate)

            manualMapInfo = this.getReceivedPayload();

        else {

            setReceivedPayload(manualMapInfo);

            manualMapInfo = null;

            this.updateMapInformation();

        }

        GridMap.autoUpdate = autoUpdate;

    }


    public boolean getCanDrawRobot() {
        return robotRenderable;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }


    public boolean getValidPosition() {
        return validPosition;
    }


    public boolean getAutoUpdate() {
        return autoUpdate;
    }


    public boolean getMapDrawn() {
        return mapRendered;
    }

    public void setUnSetCellStatus(boolean status) {
        unexploredCellStatus = status;
    }

    public boolean getUnSetCellStatus() {
        return unexploredCellStatus;
    }

    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    public boolean getExploredStatus() {
        return setExploredStatus;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setStartCoordinatesStatus(boolean status) {
        startCoordinatesStatus = status;
    }


    private boolean getStartCoordinatesStatus() {
        return startCoordinatesStatus;
    }


    public void setWPStatus(boolean status) {
        setWPStatus = status;
    }


    public void setEndCoordinates(int column, int row) {

        row = this.convertRow(row);

        for (int x = column - 1; x <= column + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");

    }

    public void setStartCoordinates(int column, int row) {

        startCoordinates[0] = column;

        startCoordinates[1] = row;

        if (this.getStartCoordinatesStatus())

            this.setCurCoordinates(column, row, "right");

    }


    private int[] getStartCoordinates() {
        return startCoordinates;
    }


    public void setCurCoordinates(int column, int row, String direction) {

        currentCoordinates[0] = column;

        currentCoordinates[1] = row;

        this.setRobotDirection(direction);

        this.updateRobotAxis(column - 1, row - 1);


        row = this.convertRow(row);

        for (int x = column - 1; x <= column + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");

    }

    private void updateRobotAxis(int column, int row) {

        TextView xAxisTextView = ((Activity) this.getContext()).findViewById(R.id.xAxisTextView);

        TextView yAxisTextView = ((Activity) this.getContext()).findViewById(R.id.yAxisTextView);

        xAxisTextView.setText(String.valueOf(column));

        yAxisTextView.setText(String.valueOf(row));

    }

    public int[] getCurrentCoordinates() {

        return currentCoordinates;

    }


    public void setRobotDirection(String direction) {

        this.sharedPreferences();

        robotFacing = direction;

        editor.putString("direction", direction);

        editor.commit();

        this.invalidate();

    }


    public String getRobotFacing() {
        return robotFacing;
    }


    private void setWOCoordinates(int column, int row) {

        wpCoordinates[0] = column;

        wpCoordinates[1] = row;

        row = this.convertRow(row);

        cells[column][row].setType("waypoint");

        String wp_x = "" + (wpCoordinates[0] - 1);

        String wp_y = "" + (wpCoordinates[1] - 1);

        if ((wpCoordinates[0] - 1) < 10) {
            wp_x = "0" + (wpCoordinates[0] - 1);
        }

        if ((wpCoordinates[1] - 1) < 10)
            wp_y = "0" + (wpCoordinates[1] - 1);

        MainActivity.setSPWP("1", wp_x, wp_y);

    }


    private int[] getWPCoordinates() {

        return wpCoordinates;

    }


    private void setObstacleCoordinates(int column, int row) {

        int[] obstacleCoordinates = new int[]{column, row};

        GridMap.obstacleCoordinates.add(obstacleCoordinates);

        row = this.convertRow(row);

        cells[column][row].setType("obstacle");

    }

    private ArrayList<int[]> getObstacleCoordinates() {
        return obstacleCoordinates;
    }

    public void moveRobot(String facing) {

        setValidPosition(false);

        int[] curCoordinates = this.getCurrentCoordinates();

        ArrayList<int[]> obstacleCoordinates = this.getObstacleCoordinates();

        this.setOldRobotCoordinates(curCoordinates[0], curCoordinates[1]);

        int[] previousRobotCoordinates = this.getPrevRobotCoordinates();

        String robotFacing = getRobotFacing();

        String manualFacing = robotFacing;


        switch (robotFacing) {

            case "up":

                switch (facing) {

                    case "forward":
                        if (curCoordinates[1] != 19) {
                            curCoordinates[1] += 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotFacing = "right";
                        break;

                    case "back":
                        if (curCoordinates[1] != 2) {
                            curCoordinates[1] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotFacing = "left";
                        break;

                    default:
                        robotFacing = "error up";
                        break;
                }
                break;

            case "right":
                switch (facing) {

                    case "forward":
                        if (curCoordinates[0] != 14) {
                            curCoordinates[0] += 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotFacing = "down";
                        break;

                    case "back":
                        if (curCoordinates[0] != 2) {
                            curCoordinates[0] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotFacing = "up";
                        break;

                    default:
                        robotFacing = "error right";
                }
                break;

            case "down":

                switch (facing) {

                    case "forward":
                        if (curCoordinates[1] != 2) {
                            curCoordinates[1] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotFacing = "left";
                        break;

                    case "back":
                        if (curCoordinates[1] != 19) {
                            curCoordinates[1] += 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotFacing = "right";
                        break;

                    default:
                        robotFacing = "error down";
                }
                break;

            case "left":
                switch (facing) {
                    case "forward":
                        if (curCoordinates[0] != 2) {
                            curCoordinates[0] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotFacing = "up";
                        break;

                    case "back":
                        if (curCoordinates[0] != 14) {
                            curCoordinates[0] += 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotFacing = "down";
                        break;

                    default:
                        robotFacing = "error left";
                }
                break;

            default:
                robotFacing = "error";
                break;
        }

        if (getValidPosition())
            for (int x = curCoordinates[0] - 1; x <= curCoordinates[0] + 1; x++) {
                for (int y = curCoordinates[1] - 1; y <= curCoordinates[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoordinates.size(); i++) {
                        if (obstacleCoordinates.get(i)[0] != x || obstacleCoordinates.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoordinates(curCoordinates[0], curCoordinates[1], robotFacing);

        else {

            if (facing.equals("forward") || facing.equals("back"))
                robotFacing = manualFacing;

            this.setCurCoordinates(previousRobotCoordinates[0], previousRobotCoordinates[1], robotFacing);
        }

        this.invalidate();

    }


    private void setOldRobotCoordinates(int oldCol, int oldRow) {

        previousCoordinates[0] = oldCol;

        previousCoordinates[1] = oldRow;

        oldRow = this.convertRow(oldRow);

        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");

    }


    private int[] getPrevRobotCoordinates() {
        return previousCoordinates;
    }


    private void setImageCoordinate(int column, int row, String imageType) {

        column += 1;

        row += 1;

        String[] imageCoordinates = new String[3];

        imageCoordinates[0] = String.valueOf(column);

        imageCoordinates[1] = String.valueOf(row);

        imageCoordinates[2] = imageType;

        boolean update = true;

        for (int i = 0; i < this.getImageCoordinates().size(); i++)
            if (this.getImageCoordinates().get(i)[0].equals(imageCoordinates[0]) && this.getImageCoordinates().get(i)[1].equals(imageCoordinates[1]) && this.getImageCoordinates().get(i)[1].equals(imageCoordinates[1]))
                update = false;


        if (update) {

            if (cells[column][20 - row].type.equals("obstacle")) {

                try {

                    this.getImageCoordinates().add(imageCoordinates);

                    this.sharedPreferences();

                    String message = "(" + (column - 1) + ", " + (row - 1) + ", " + Integer.parseInt(imageCoordinates[2], 16) + ")";

                    editor.putString("image", sharedPreferences.getString("image", "") + "\n " + message);

                    editor.commit();

                    row = convertRow(row);

                    cells[column][row].setType("image");

                } catch (Exception e) {

                }

            }
        }

    }


    private ArrayList<String[]> getImageCoordinates() {
        return imageCoordinates;
    }


    private void renderCell(Canvas canvas) {

        for (int x = 1; x <= COLUMN; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getImageCoordinates().size(); i++)
                    canvas.drawRect(cells[x][y].fromX, cells[x][y].fromY, cells[x][y].toX, cells[x][y].toY, cells[x][y].paint);

    }


    private void renderAxisLabels(Canvas canvas) {

        for (int x = 1; x <= COLUMN; x++) {

            if (x > 10)
                canvas.drawText(Integer.toString(x - 1), cells[x][20].fromX + (cellsSize / 5), cells[x][20].fromY + (cellsSize / 3), whiteColor);
            else
                canvas.drawText(Integer.toString(x - 1), cells[x][20].fromX + (cellsSize / 3), cells[x][20].fromY + (cellsSize / 3), whiteColor);
        }

        for (int y = 0; y < ROW; y++) {

            if ((20 - (y + 1)) > 9)
                canvas.drawText(Integer.toString(20 - (y + 1)), cells[0][y].fromX + (cellsSize / 2), cells[0][y].fromY + (cellsSize / 1.5f), whiteColor);
            else
                canvas.drawText(Integer.toString(20 - (y + 1)), cells[0][y].fromX + (cellsSize / 1.5f), cells[0][y].fromY + (cellsSize / 1.5f), whiteColor);
        }

    }


    private void renderRobot(Canvas canvas, int[] curCoord) {

        int internalRowCoordinates = this.convertRow(curCoord[1]);

        for (int y = internalRowCoordinates; y <= internalRowCoordinates + 1; y++)
            canvas.drawLine(cells[curCoord[0] - 1][y].fromX, cells[curCoord[0] - 1][y].fromY - (cellsSize / 30), cells[curCoord[0] + 1][y].toX, cells[curCoord[0] + 1][y].fromY - (cellsSize / 30), robotColor);

        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            canvas.drawLine(cells[x][internalRowCoordinates - 1].fromX - (cellsSize / 30) + cellsSize, cells[x][internalRowCoordinates - 1].fromY, cells[x][internalRowCoordinates + 1].fromX - (cellsSize / 30) + cellsSize, cells[x][internalRowCoordinates + 1].toY, robotColor);


        switch (this.getRobotFacing()) {

            case "up":
                canvas.drawLine(cells[curCoord[0] - 1][internalRowCoordinates + 1].fromX, cells[curCoord[0] - 1][internalRowCoordinates + 1].toY, (cells[curCoord[0]][internalRowCoordinates - 1].fromX + cells[curCoord[0]][internalRowCoordinates - 1].toX) / 2, cells[curCoord[0]][internalRowCoordinates - 1].fromY, blackColor);
                canvas.drawLine((cells[curCoord[0]][internalRowCoordinates - 1].fromX + cells[curCoord[0]][internalRowCoordinates - 1].toX) / 2, cells[curCoord[0]][internalRowCoordinates - 1].fromY, cells[curCoord[0] + 1][internalRowCoordinates + 1].toX, cells[curCoord[0] + 1][internalRowCoordinates + 1].toY, blackColor);
                break;

            case "down":
                canvas.drawLine(cells[curCoord[0] - 1][internalRowCoordinates - 1].fromX, cells[curCoord[0] - 1][internalRowCoordinates - 1].fromY, (cells[curCoord[0]][internalRowCoordinates + 1].fromX + cells[curCoord[0]][internalRowCoordinates + 1].toX) / 2, cells[curCoord[0]][internalRowCoordinates + 1].toY, blackColor);
                canvas.drawLine((cells[curCoord[0]][internalRowCoordinates + 1].fromX + cells[curCoord[0]][internalRowCoordinates + 1].toX) / 2, cells[curCoord[0]][internalRowCoordinates + 1].toY, cells[curCoord[0] + 1][internalRowCoordinates - 1].toX, cells[curCoord[0] + 1][internalRowCoordinates - 1].fromY, blackColor);
                break;

            case "right":
                canvas.drawLine(cells[curCoord[0] - 1][internalRowCoordinates - 1].fromX, cells[curCoord[0] - 1][internalRowCoordinates - 1].fromY, cells[curCoord[0] + 1][internalRowCoordinates].toX, cells[curCoord[0] + 1][internalRowCoordinates - 1].toY + (cells[curCoord[0] + 1][internalRowCoordinates].toY - cells[curCoord[0] + 1][internalRowCoordinates - 1].toY) / 2, blackColor);
                canvas.drawLine(cells[curCoord[0] + 1][internalRowCoordinates].toX, cells[curCoord[0] + 1][internalRowCoordinates - 1].toY + (cells[curCoord[0] + 1][internalRowCoordinates].toY - cells[curCoord[0] + 1][internalRowCoordinates - 1].toY) / 2, cells[curCoord[0] - 1][internalRowCoordinates + 1].fromX, cells[curCoord[0] - 1][internalRowCoordinates + 1].toY, blackColor);
                break;

            case "left":
                canvas.drawLine(cells[curCoord[0] + 1][internalRowCoordinates - 1].toX, cells[curCoord[0] + 1][internalRowCoordinates - 1].fromY, cells[curCoord[0] - 1][internalRowCoordinates].fromX, cells[curCoord[0] - 1][internalRowCoordinates - 1].toY + (cells[curCoord[0] - 1][internalRowCoordinates].toY - cells[curCoord[0] - 1][internalRowCoordinates - 1].toY) / 2, blackColor);
                canvas.drawLine(cells[curCoord[0] - 1][internalRowCoordinates].fromX, cells[curCoord[0] - 1][internalRowCoordinates - 1].toY + (cells[curCoord[0] - 1][internalRowCoordinates].toY - cells[curCoord[0] - 1][internalRowCoordinates - 1].toY) / 2, cells[curCoord[0] + 1][internalRowCoordinates + 1].toX, cells[curCoord[0] + 1][internalRowCoordinates + 1].toY, blackColor);
                break;

            default:
                break;
        }

    }


    private void renderImages(Canvas canvas, ArrayList<String[]> imageCoordinates) {

        RectF rect;

        for (int i = 0; i < imageCoordinates.size(); i++) {

            if (!imageCoordinates.get(i)[2].equals("placeholder")) {

                int column = Integer.parseInt(imageCoordinates.get(i)[0]);

                int row = convertRow(Integer.parseInt(imageCoordinates.get(i)[1]));

                rect = new RectF(column * cellsSize, row * cellsSize, (column + 1) * cellsSize, (row + 1) * cellsSize);

                switch (imageCoordinates.get(i)[2]) {

                    case "1":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.up);
                        break;

                    case "2":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.down);
                        break;

                    case "3":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.right);
                        break;

                    case "4":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.left);
                        break;

                    case "5":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.circle);
                        break;

                    case "6":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.one);
                        break;

                    case "7":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.two);
                        break;

                    case "8":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.three);
                        break;

                    case "9":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.four);
                        break;

                    case "A":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.five);
                        break;

                    case "B":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.letter_a);
                        break;

                    case "C":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.letter_b);
                        break;

                    case "D":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.letter_c);
                        break;

                    case "E":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.letter_d);
                        break;

                    case "F":
                        image = BitmapFactory.decodeResource(getResources(), R.drawable.letter_e);
                        break;

                    default:
                        break;
                }

                canvas.drawBitmap(image, null, rect, null);
            }

        }
    }


    private class Cell {
        float toX;
        float toY;
        float fromX;
        float fromY;

        Paint paint;
        String type;

        private Cell(float fromX, float fromY, float toX, float endY, Paint paint, String type) {

            this.fromX = fromX;

            this.fromY = fromY;

            this.toX = toX;

            this.toY = endY;

            this.paint = paint;

            this.type = type;

        }

        public void setType(String type) {

            this.type = type;

            switch (type) {

                case "obstacle":
                    this.paint = obstacleColor;
                    break;

                case "robot":
                    this.paint = robotColor;
                    break;

                case "end":
                    this.paint = endColor;
                    break;

                case "start":
                    this.paint = startColor;
                    break;

                case "waypoint":
                    this.paint = wpColor;
                    break;

                case "unexplored":
                    this.paint = unexploredColor;
                    break;

                case "explored":
                    this.paint = exploredColor;
                    break;

                case "image":
                    this.paint = imageColor;
                    break;

                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;

                default:
                    break;
            }
        }
    }


    private void calculateDimension() {
        this.setCellSize(getWidth() / (COLUMN + 1));
    }


    private void setCellSize(float cellSize) {
        GridMap.cellsSize = cellSize;
    }


    private float getCellSize() {
        return cellsSize;
    }


    public void updateMapInformation() throws JSONException {

        JSONObject mapInformation = this.getReceivedPayload();

        JSONArray mapInfoJsonArray;

        JSONObject mapInfoJsonObject;

        String explored;


        if (mapInformation == null || mapInformation.names() == null)
            return;

        for (int i = 0; i < mapInformation.names().length(); i++) {

            switch (mapInformation.names().getString(i)) {


                case "map":

                    mapInfoJsonArray = mapInformation.getJSONArray("map");

                    mapInfoJsonObject = mapInfoJsonArray.getJSONObject(0);


                    try {

                        if (robotRenderable)
                            this.setOldRobotCoordinates(currentCoordinates[0], currentCoordinates[1]);

                        String direction = mapInfoJsonObject.getString("robotFacing");

                        switch (direction) {

                            case "0":
                                direction = "up";
                                break;

                            case "1":
                                direction = "down";
                                break;

                            case "2":
                                direction = "right";
                                break;

                            case "3":
                                direction = "left";
                                break;
                        }

                        this.setCurCoordinates(mapInfoJsonObject.getInt("robotX") + 1, mapInfoJsonObject.getInt("robotY") + 1, direction);

                        robotRenderable = true;

                    } catch (JSONException e) {

                        e.printStackTrace();

                    }


                    explored = mapInfoJsonObject.getString("explored");

                    explored = "F" + explored;

                    explored = new BigInteger(explored, 16).toString(2);

                    explored = explored.substring(4);

                    int x, y;

                    for (int j = 0; j < explored.length(); j += 2) {

                        y = 19 - (j / 30);
                        x = 1 + (j / 2) - ((19 - y) * 15);

                        if (!cells[x][y].type.equals("robot")) {


                            if ((String.valueOf(explored.charAt(j))).equals("1") && (String.valueOf(explored.charAt(j + 1))).equals("1")) {

                                cells[x][y].setType("explored");


                            } else if ((String.valueOf(explored.charAt(j))).equals("1")) {

                                cells[x][y].setType("explored");


                            } else if ((String.valueOf(explored.charAt(j + 1))).equals("1")) {

                                this.setObstacleCoordinates(x, 20 - y);


                            } else {

                                cells[x][y].setType("unexplored");
                            }

                        }

                    }

                    break;

                case "image":
                    mapInfoJsonArray = mapInformation.getJSONArray("image");

                    for (int j = 0; j < mapInfoJsonArray.length(); j++) {

                        mapInfoJsonObject = mapInfoJsonArray.getJSONObject(j);

                        String imageString = mapInfoJsonObject.getString("imageString");

                        if (imageString.length() != 0) {

                            while (imageString.length() > 0) {

                                String nextChunk = imageString.substring(0, 5);

                                String imageX = nextChunk.substring(0, 2);

                                String imageY = nextChunk.substring(2, 4);

                                String imageType = nextChunk.substring(4);

                                this.setImageCoordinate(Integer.parseInt(imageX), Integer.parseInt(imageY), imageType);

                                imageString = imageString.substring(5);
                            }
                        }


                    }
                    break;

                default:
                    break;
            }

        }

        this.invalidate();
    }

    public void setReceivedPayload(JSONObject receivedPayload) {

        GridMap.receivedPayload = receivedPayload;

        manualMapInfo = receivedPayload;

    }


    public JSONObject getReceivedPayload() {
        return receivedPayload;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN && !this.getAutoUpdate()) {

            int column = (int) (event.getX() / cellsSize);

            int row = this.convertRow((int) (event.getY() / cellsSize));


            ToggleButton spToggle = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);

            ToggleButton wpToggle = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);

            if (startCoordinatesStatus) {

                if (robotRenderable) {

                    int[] startCoordinates = this.getStartCoordinates();

                    if (startCoordinates[0] >= 2 && startCoordinates[1] >= 2) {

                        startCoordinates[1] = this.convertRow(startCoordinates[1]);

                        for (int x = startCoordinates[0] - 1; x <= startCoordinates[0] + 1; x++)
                            for (int y = startCoordinates[1] - 1; y <= startCoordinates[1] + 1; y++)
                                cells[x][y].setType("unexplored");

                    }

                } else {

                    robotRenderable = true;

                }

                this.setStartCoordinates(column, row);

                startCoordinatesStatus = false;

                String sp_x = "" + (column - 1);

                String sp_y = "" + (row - 1);

                if ((column - 1) < 10) {

                    sp_x = "0" + (column - 1);

                }

                if ((row - 1) < 10)
                    sp_y = "0" + (row - 1);

                MainActivity.setSPWP("0", sp_x, sp_y);

                updateRobotAxis(column - 1, row - 1);

                if (spToggle.isChecked())
                    spToggle.toggle();

                this.invalidate();

                return true;

            }

            if (setWPStatus) {

                int[] waypointCoordinates = this.getWPCoordinates();

                if (waypointCoordinates[0] >= 1 && waypointCoordinates[1] >= 1)
                    cells[waypointCoordinates[0]][this.convertRow(waypointCoordinates[1])].setType("unexplored");

                setWPStatus = false;

                try {

                    this.setWOCoordinates(column, row);

                } catch (Exception e) {

                    e.printStackTrace();

                }

                if (wpToggle.isChecked())
                    wpToggle.toggle();

                this.invalidate();

                return true;

            }

            if (setObstacleStatus) {

                this.setObstacleCoordinates(column, row);

                this.invalidate();

                return true;
            }

            if (setExploredStatus) {

                cells[column][20 - row].setType("explored");

                this.invalidate();

                return true;
            }

            if (unexploredCellStatus) {

                ArrayList<int[]> obstacleCoordinates = this.getObstacleCoordinates();

                cells[column][20 - row].setType("unexplored");

                for (int i = 0; i < obstacleCoordinates.size(); i++)
                    if (obstacleCoordinates.get(i)[0] == column && obstacleCoordinates.get(i)[1] == row)
                        obstacleCoordinates.remove(i);

                this.invalidate();

                return true;
            }
        }

        return false;
    }


    public void toggleCheckedBtn(String buttonName) {

        ToggleButton spToggle = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);

        ToggleButton wpToggle = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);

        Button obstacleImageBtn = ((Activity) this.getContext()).findViewById(R.id.obstacleImageBtn);

        Button exploredImageBtn = ((Activity) this.getContext()).findViewById(R.id.exploredImageBtn);

        Button clearImageBtn = ((Activity) this.getContext()).findViewById(R.id.unexploredImageBtn);

        if (!buttonName.equals("spToggle"))
            if (spToggle.isChecked()) {
                this.setStartCoordinatesStatus(false);
                spToggle.toggle();
            }

        if (!buttonName.equals("wpToggle"))
            if (wpToggle.isChecked()) {
                this.setWPStatus(false);
                wpToggle.toggle();
            }

        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);

        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);

        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }


    public JSONObject getCreateJsonObject() {

        String explored = "11";

        String obstacle = "";

        String hex = "";

        String hexExplored;

        BigInteger hexBigIntegerObstacle, hexBigIntegerExplored;

        int[] wpCoordinates = this.getWPCoordinates();

        int[] currentCoordinates = this.getCurrentCoordinates();

        String robotFacing = this.getRobotFacing();

        List<int[]> obstacleCoordinates = new ArrayList<>(this.getObstacleCoordinates());

        List<String[]> imageCoordinates = new ArrayList<>(this.getImageCoordinates());

        TextView robotStatus = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);


        JSONObject map = new JSONObject();

        for (int y = ROW - 1; y >= 0; y--) {
            for (int x = 1; x <= COLUMN; x++) {
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image"))
                    explored = explored + "1";
                else
                    explored = explored + "0";
            }
        }

        explored = explored + "11";


        hexBigIntegerExplored = new BigInteger(explored, 2);

        hexExplored = hexBigIntegerExplored.toString(16);


        for (int y = ROW - 1; y >= 0; y--) {

            for (int x = 1; x <= COLUMN; x++) {

                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot")) {

                    obstacle = obstacle + "0";

                } else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image")) {

                    obstacle = obstacle + "1";
                }

            }

        }

        while ((obstacle.length() % 8) != 0) {

            obstacle = obstacle + "0";

        }

        if (!obstacle.equals("")) {

            hexBigIntegerObstacle = new BigInteger(obstacle, 2);

            hex = hexBigIntegerObstacle.toString(16);

            if (hex.length() % 2 != 0)

                hex = "0" + hex;

        }

        try {

            map.put("explored", hexExplored);

            map.put("length", obstacle.length());

            if (!obstacle.equals(""))

                map.put("obstacle", hex);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        JSONArray jsonMap = new JSONArray();

        jsonMap.put(map);


        JSONArray jsonRobot = new JSONArray();

        if (currentCoordinates[0] >= 2 && currentCoordinates[1] >= 2)

            try {

                JSONObject robot = new JSONObject();

                robot.put("x", currentCoordinates[0]);

                robot.put("y", currentCoordinates[1]);

                robot.put("direction", robotFacing);

                jsonRobot.put(robot);

            } catch (JSONException e) {

                e.printStackTrace();
            }


        JSONArray jsonWaypoint = new JSONArray();

        if (wpCoordinates[0] >= 1 && wpCoordinates[1] >= 1)

            try {

                JSONObject wp = new JSONObject();

                wp.put("x", wpCoordinates[0]);

                wp.put("y", wpCoordinates[1]);

                setWPStatus = true;

                jsonWaypoint.put(wp);

            } catch (JSONException e) {

                e.printStackTrace();
            }


        JSONArray jsonObstacle = new JSONArray();

        for (int i = 0; i < obstacleCoordinates.size(); i++)

            try {

                JSONObject ob = new JSONObject();

                ob.put("x", obstacleCoordinates.get(i)[0]);

                ob.put("y", obstacleCoordinates.get(i)[1]);

                jsonObstacle.put(ob);

            } catch (JSONException e) {

                e.printStackTrace();
            }


        JSONArray jsonImage = new JSONArray();

        for (int i = 0; i < imageCoordinates.size(); i++) {

            try {

                JSONObject image = new JSONObject();

                image.put("imageX", Integer.parseInt(imageCoordinates.get(i)[0]));

                image.put("imageY", Integer.parseInt(imageCoordinates.get(i)[1]));

                image.put("imageType", imageCoordinates.get(i)[2]);

                jsonImage.put(image);

            } catch (JSONException e) {

                e.printStackTrace();
            }
        }


        JSONArray jsonStatus = new JSONArray();

        try {

            JSONObject status = new JSONObject();

            status.put("status", robotStatus.getText().toString());

            jsonStatus.put(status);

        } catch (JSONException e) {

            e.printStackTrace();
        }


        mapInfo = new JSONObject();

        try {
            mapInfo.put("robot", jsonRobot);

            mapInfo.put("map", jsonMap);


            if (setWPStatus) {

                mapInfo.put("waypoint", jsonWaypoint);

                setWPStatus = false;
            }

            mapInfo.put("obstacle", jsonObstacle);

            mapInfo.put("image", jsonImage);

            mapInfo.put("status", jsonStatus);

        } catch (JSONException e) {

            e.printStackTrace();

        }

        return mapInfo;
    }


    public void resetMap() {

        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);

        Switch phoneTiltSwitch = ((Activity) this.getContext()).findViewById(R.id.phoneTiltSwitch);

        Button manualUpdateBtn = ((Activity) this.getContext()).findViewById(R.id.manualUpdateBtn);

        ToggleButton manualAutoToggleBtn = ((Activity) this.getContext()).findViewById(R.id.manualAutoToggleBtn);

        if (phoneTiltSwitch.isChecked()) {

            phoneTiltSwitch.toggle();

            phoneTiltSwitch.setText("TILT OFF");

        }
        if (manualAutoToggleBtn.isChecked())
            manualAutoToggleBtn.toggle();

        this.toggleCheckedBtn("None");

        manualUpdateBtn.setEnabled(true);

        robotStatusTextView.setText("None");

        sharedPreferences();

        updateRobotAxis(0, 0);

        editor.putString("receivedText", "");

        editor.putString("sentText", "");

        editor.putString("image", "");


        editor.commit();



        manualMapInfo = null;
        obstacleCoordinates = new ArrayList<>();

        wpCoordinates = new int[]{-1, -1};

        mapRendered = false;

        robotRenderable = false;

        validPosition = false;

        startCoordinates = new int[]{-1, -1};

        currentCoordinates = new int[]{-1, -1};

        previousCoordinates = new int[]{-1, -1};

        robotFacing = "None";

        autoUpdate = false;

        imageCoordinates = new ArrayList<>();

        receivedPayload = null;

        this.invalidate();
    }


    private void sharedPreferences() {

        sharedPreferences = this.getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        editor = sharedPreferences.edit();
        
    }
}