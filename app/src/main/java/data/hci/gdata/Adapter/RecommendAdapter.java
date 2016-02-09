package data.hci.gdata.Adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import data.hci.gdata.R;

/**
 * Created by user on 2016-01-26.
 */
public class RecommendAdapter extends RecyclerView.Adapter<RecommendAdapter.ViewHolder> {
    List<RecommendItem> item;
    Handler handler;
    Bitmap bitmap=null;

    //Adapter의 생성자
    public RecommendAdapter(List<RecommendItem> recommendItems){
        item = recommendItems;
        handler = new Handler();
    }

    //ViewHolder를 생성
    @Override
    public RecommendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_cardview, parent, false);

        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    //만들어진 ViewHolder에 데이터를 넣는 작업, ListView의 getView와 동일
    @Override
    public void onBindViewHolder(RecommendAdapter.ViewHolder holder, int position) {
        holder.title.setText(item.get(position).getTitle());
        getBitmap(holder, position);
    }

    //img url을 안드로이드에 로딩하기 위한 함수
    public void getBitmap(final RecommendAdapter.ViewHolder holder,final int position){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                //url로 부터 비트맵을 로드한다.
                try{
                    URL url = new URL(item.get(position).getImage());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();

                    bitmap = BitmapFactory.decodeStream(conn.getInputStream());

                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(conn != null)
                        conn.disconnect();
                }
                //핸들러르 통해 뷰홀더의 이미지 업데이트
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ViewHolder tmpHolder = holder;
                        tmpHolder.img.setImageBitmap(bitmap);
                    }
                });
            }
        }).start();
    }

    /**
     * 사이즈 반환
     * */
    @Override
    public int getItemCount() {
        return item.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView img;
        TextView title;
        CardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            img = (ImageView)itemView.findViewById(R.id.card_img);
            title = (TextView)itemView.findViewById(R.id.card_title);
            cardView = (CardView)itemView.findViewById(R.id.cardview);
        }
    }
}
