/*
 * This file is a part of the Nadeshiko project. Nadeshiko is free software, licensed under the MIT license.
 *
 * Usage of these works (including, yet not limited to, reuse, modification, copying, distribution, and selling) is
 * permitted, provided that the relevant copyright notice and permission notice (as specified in LICENSE) shall be
 * included in all copies or substantial portions of this software.
 *
 * These works are provided "AS IS" with absolutely no warranty of any kind, either expressed or implied.
 *
 * You should have received a copy of the MIT License alongside this software; refer to LICENSE for information.
 * If not, refer to https://mit-license.org.
 */

package io.nadeshiko.nadeshiko.monitoring;

import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class used to execute Discord Webhooks with low effort
 * @author k3kdude
 * @see <a href="https://gist.github.com/k3kdude/fba6f6b37594eae3d6f9475330733bdb">
 *         https://gist.github.com/k3kdude/fba6f6b37594eae3d6f9475330733bdb</a>
 */
@Setter
@SuppressWarnings("unused")
public class DiscordWebhook {

	private final String url;
	private String content;
	private String username;
	private String avatarUrl;
	private boolean tts;
	private List<EmbedObject> embeds = new ArrayList<>();

	/**
	 * Constructs a new DiscordWebhook instance
	 *
	 * @param url The webhook URL obtained in Discord
	 */
	public DiscordWebhook(String url) {
		this.url = url;
	}

	public void addEmbed(EmbedObject embed) {
		this.embeds.add(embed);
	}

	public void execute() throws IOException {
		if (this.content == null && this.embeds.isEmpty()) {
			throw new IllegalArgumentException("Set content or add at least one EmbedObject");
		}

		JSONObject json = new JSONObject();

		json.put("content", this.content);
		json.put("username", this.username);
		json.put("avatar_url", this.avatarUrl);
		json.put("tts", this.tts);

		if (!this.embeds.isEmpty()) {
			List<JSONObject> embedObjects = new ArrayList<>();

			for (EmbedObject embed : this.embeds) {
				JSONObject jsonEmbed = new JSONObject();

				jsonEmbed.put("title", embed.getTitle());
				jsonEmbed.put("description", embed.getDescription());
				jsonEmbed.put("url", embed.getUrl());

				if (embed.getColor() != null) {
					Color color = embed.getColor();
					int rgb = color.getRed();
					rgb = (rgb << 8) + color.getGreen();
					rgb = (rgb << 8) + color.getBlue();

					jsonEmbed.put("color", rgb);
				}

				EmbedObject.Footer footer = embed.getFooter();
				EmbedObject.Image image = embed.getImage();
				EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
				EmbedObject.Author author = embed.getAuthor();
				List<EmbedObject.Field> fields = embed.getFields();

				if (footer != null) {
					JSONObject jsonFooter = new JSONObject();

					jsonFooter.put("text", footer.text());
					jsonFooter.put("icon_url", footer.iconUrl());
					jsonEmbed.put("footer", jsonFooter);
				}

				if (image != null) {
					JSONObject jsonImage = new JSONObject();

					jsonImage.put("url", image.url());
					jsonEmbed.put("image", jsonImage);
				}

				if (thumbnail != null) {
					JSONObject jsonThumbnail = new JSONObject();

					jsonThumbnail.put("url", thumbnail.url());
					jsonEmbed.put("thumbnail", jsonThumbnail);
				}

				if (author != null) {
					JSONObject jsonAuthor = new JSONObject();

					jsonAuthor.put("name", author.name());
					jsonAuthor.put("url", author.url());
					jsonAuthor.put("icon_url", author.iconUrl());
					jsonEmbed.put("author", jsonAuthor);
				}

				List<JSONObject> jsonFields = new ArrayList<>();
				for (EmbedObject.Field field : fields) {
					JSONObject jsonField = new JSONObject();

					jsonField.put("name", field.name());
					jsonField.put("value", field.value());
					jsonField.put("inline", field.inline());

					jsonFields.add(jsonField);
				}

				jsonEmbed.put("fields", jsonFields.toArray());
				embedObjects.add(jsonEmbed);
			}

			json.put("embeds", embedObjects.toArray());
		}

		URL url = new URL(this.url);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.addRequestProperty("Content-Type", "application/json");
		connection.addRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");

		OutputStream stream = connection.getOutputStream();
		stream.write(json.toString().getBytes());
		stream.flush();
		stream.close();

		connection.getInputStream().close(); //I'm not sure why but it doesn't work without getting the InputStream
		connection.disconnect();
	}

	@Getter
	@Setter
	public static class EmbedObject {
		private String title;
		private String description;
		private String url;
		private Color color;

		private Footer footer;
		private Thumbnail thumbnail;
		private Image image;
		private Author author;
		private final List<Field> fields = new ArrayList<>();

		public void setFooter(String text, String icon) {
			this.footer = new Footer(text, icon);
		}

		public void setAuthor(String name, String url, String icon) {
			this.author = new Author(name, url, icon);
		}

		public void setImage(String url) {
			this.image = new Image(url);
		}

		public void addField(String name, String value, boolean inline) {
			this.fields.add(new Field(name, value, inline));
		}

		public record Footer(String text, String iconUrl) {
		}

		public record Thumbnail(String url) {
		}

		public record Image(String url) {
		}

		public record Author(String name, String url, String iconUrl) {
		}

		public record Field(String name, String value, boolean inline) {
		}
	}

	private static class JSONObject {

		private final HashMap<String, Object> map = new HashMap<>();

		void put(String key, Object value) {
			if (value != null) {
				map.put(key, value);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			Set<Map.Entry<String, Object>> entrySet = map.entrySet();
			builder.append("{");

			int i = 0;
			for (Map.Entry<String, Object> entry : entrySet) {
				Object val = entry.getValue();
				builder.append(quote(entry.getKey())).append(":");

				if (val instanceof String) {
					builder.append(quote(String.valueOf(val)));
				} else if (val instanceof Integer) {
					builder.append(Integer.valueOf(String.valueOf(val)));
				} else if (val instanceof Boolean) {
					builder.append(val);
				} else if (val instanceof JSONObject) {
					builder.append(val.toString());
				} else if (val.getClass().isArray()) {
					builder.append("[");
					int len = Array.getLength(val);
					for (int j = 0; j < len; j++) {
						builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
					}
					builder.append("]");
				}

				builder.append(++i == entrySet.size() ? "}" : ",");
			}

			return builder.toString();
		}

		private String quote(String string) {
			return "\"" + string + "\"";
		}
	}

}