package data.hci.gdata.Adapter;

/**
 * Created by user on 2016-01-26.
 */
public class RecommendItem {
    String image;
    String title;

    String getImage()
    {
        return this.image;
    }

    String getTitle()
    {
        return this.title;
    }

    public RecommendItem(String image, String title)
    {
        this.image=image;
        this.title=title;
    }
}
