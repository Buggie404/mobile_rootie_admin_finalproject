package com.veganbeauty.admin.features.product.add;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.entities.KeyIngredient;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.databinding.ProductFragmentAddBinding;
import com.veganbeauty.admin.features.product.ProductViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProductAddFragment extends RootieAdminFragment {

    private static final String ARG_PRODUCT_ID = "arg_product_id";

    private ProductFragmentAddBinding binding;
    private ProductViewModel viewModel;
    private final List<String> selectedImages = new ArrayList<>();
    private final Set<String> selectedSubcategoriesSet = new HashSet<>();

    private String ingredientsImageUrl = "";
    private String usageMediaUrl = "";

    private String editingProductId = null;
    private boolean isEditMode = false;
    private boolean isDataLoaded = false;
    private String selectedCategoryName = "";

    // Image Picker & Camera Launchers
    private final ActivityResultLauncher<String> pickAlbumImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImages.add(uri.toString());
                    renderImages();
                }
            }
    );

    private final ActivityResultLauncher<Void> captureAlbumImageLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    Uri uri = saveBitmapToCache(bitmap);
                    selectedImages.add(uri.toString());
                    renderImages();
                }
            }
    );

    private final ActivityResultLauncher<String> pickIngredientsImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    ingredientsImageUrl = uri.toString();
                    binding.imvIngredientsImage.setVisibility(View.VISIBLE);
                    ImageUtils.loadImage(requireContext(), binding.imvIngredientsImage, uri, R.color.gray_light);
                }
            }
    );

    private final ActivityResultLauncher<Void> captureIngredientsImageLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    Uri uri = saveBitmapToCache(bitmap);
                    ingredientsImageUrl = uri.toString();
                    binding.imvIngredientsImage.setVisibility(View.VISIBLE);
                    ImageUtils.loadImage(requireContext(), binding.imvIngredientsImage, uri, R.color.gray_light);
                }
            }
    );

    private final ActivityResultLauncher<String> pickUsageMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    usageMediaUrl = uri.toString();
                    binding.imvUsageMedia.setVisibility(View.VISIBLE);
                    ImageUtils.loadImage(requireContext(), binding.imvUsageMedia, uri, R.color.gray_light);
                }
            }
    );

    private final ActivityResultLauncher<Void> captureUsageMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    Uri uri = saveBitmapToCache(bitmap);
                    usageMediaUrl = uri.toString();
                    binding.imvUsageMedia.setVisibility(View.VISIBLE);
                    ImageUtils.loadImage(requireContext(), binding.imvUsageMedia, uri, R.color.gray_light);
                }
            }
    );

    public static ProductAddFragment newInstance(String productId) {
        ProductAddFragment fragment = new ProductAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }

    public ProductAddFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ProductFragmentAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        viewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        // Read arguments for edit mode
        if (getArguments() != null) {
            editingProductId = getArguments().getString(ARG_PRODUCT_ID);
        }
        isEditMode = editingProductId != null && !editingProductId.isEmpty();

        if (isEditMode) {
            binding.txtTitle.setText("Chỉnh sửa sản phẩm");
        }

        // Setup Back Button
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Setup Spinners and Multi-choice Subcategory Dropdown
        setupCategoriesSpinner();

        // Setup Product Images Click (Album + Camera)
        binding.btnAddImage.setOnClickListener(v -> showImageSourceDialog(
                () -> pickAlbumImageLauncher.launch("image/*"),
                () -> captureAlbumImageLauncher.launch(null)
        ));

        // Setup Ingredients Image Click
        binding.btnUploadIngredientsImage.setOnClickListener(v -> showImageSourceDialog(
                () -> pickIngredientsImageLauncher.launch("image/*"),
                () -> captureIngredientsImageLauncher.launch(null)
        ));

        // Setup Usage Media Click
        binding.btnUploadUsageMedia.setOnClickListener(v -> showImageSourceDialog(
                () -> pickUsageMediaLauncher.launch("image/*"),
                () -> captureUsageMediaLauncher.launch(null)
        ));

        // Setup Dynamic Key Ingredients Row Add
        binding.btnAddKeyIngredient.setOnClickListener(v -> addKeyIngredientRow("", ""));

        // Setup Save Button
        binding.btnSave.setOnClickListener(v -> saveProduct());

        // Render initial empty images list
        renderImages();

        // Bind message button in header
        setupHeaderMessageButton(binding.btnMessage);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                setupCategoriesSpinner(products);
                if (isEditMode && !isDataLoaded) {
                    ProductEntity product = null;
                    for (ProductEntity p : products) {
                        if (p.getId().equals(editingProductId)) {
                            product = p;
                            break;
                        }
                    }
                    if (product != null) {
                        isDataLoaded = true;
                        populateProductData(product, products);
                    }
                }
            }
        });
    }

    private void setupSubcategories(String category, List<ProductEntity> allProducts) {
        Set<String> subcategoriesSet = new HashSet<>();
        for (ProductEntity product : allProducts) {
            if (product.getCategory() != null && product.getCategory().equalsIgnoreCase(category)) {
                String sub = product.getSubcategory();
                if (sub != null) {
                    String[] parts = sub.split(",");
                    for (String part : parts) {
                        String clean = part.trim();
                        if (!clean.isEmpty()) {
                            subcategoriesSet.add(clean);
                        }
                    }
                }
            }
        }
        List<String> subs = new ArrayList<>(subcategoriesSet);

        if (subs.isEmpty()) {
            if ("Chăm sóc da".equalsIgnoreCase(category)) {
                subs.addAll(Arrays.asList("Chăm Sóc Da Mặt", "Chăm Sóc Cơ Thể", "Chăm Sóc Mái Tóc", "Chăm Sóc Môi", "Chống Nắng"));
            } else if ("Combo & Bộ Sản Phẩm".equalsIgnoreCase(category)) {
                subs.addAll(Arrays.asList("Chăm Sóc Da Mặt", "Chăm Sóc Mái Tóc"));
            } else {
                subs.addAll(Arrays.asList("Chăm sóc toàn thân", "Chăm sóc đặc biệt"));
            }
        }

        binding.layoutCategoryLevel1.setOnClickListener(v -> {
            ProductGenericBottomSheetFragment multiSelectSheet = ProductGenericBottomSheetFragment.newMultiSelectInstance(
                    "Chọn phân loại con",
                    subs,
                    selectedSubcategoriesSet,
                    selected -> {
                        selectedSubcategoriesSet.clear();
                        selectedSubcategoriesSet.addAll(selected);
                        if (selectedSubcategoriesSet.isEmpty()) {
                            binding.txtCategoryLevel1.setText("Chọn phân loại con");
                            binding.txtCategoryLevel1.setTextColor(getResources().getColor(R.color.tertiary, null));
                        } else {
                            StringBuilder sb = new StringBuilder();
                            int count = 0;
                            for (String s : selectedSubcategoriesSet) {
                                if (count > 0) sb.append(", ");
                                sb.append(s);
                                count++;
                            }
                            binding.txtCategoryLevel1.setText(sb.toString());
                            binding.txtCategoryLevel1.setTextColor(getResources().getColor(R.color.primary, null));
                        }
                    }
            );
            multiSelectSheet.show(getChildFragmentManager(), "CategoryLevel1BottomSheet");
        });
    }

    private void populateProductData(ProductEntity product, List<ProductEntity> allProducts) {
        binding.txtTitle.setText("Chỉnh sửa sản phẩm");

        binding.edtProductName.setText(product.getName());
        binding.edtSku.setText(product.getSku());
        binding.edtStock.setText(String.valueOf(product.getStock()));
        binding.edtPrice.setText(String.valueOf(product.getPrice()));
        binding.edtOriginalPrice.setText(product.getOriginalPrice() != null ? String.valueOf(product.getOriginalPrice()) : "");

        // Category Level 0
        selectedCategoryName = product.getCategory();
        binding.txtCategoryLevel0.setText(product.getCategory());
        binding.txtCategoryLevel0.setTextColor(getResources().getColor(R.color.primary, null));

        // Subcategory Level 1
        selectedSubcategoriesSet.clear();
        if (product.getSubcategory() != null) {
            String[] subsList = product.getSubcategory().split(",");
            for (String sub : subsList) {
                String clean = sub.trim();
                if (!clean.isEmpty()) {
                    selectedSubcategoriesSet.add(clean);
                }
            }
        }

        if (!selectedSubcategoriesSet.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String s : selectedSubcategoriesSet) {
                if (count > 0) sb.append(", ");
                sb.append(s);
                count++;
            }
            binding.txtCategoryLevel1.setText(sb.toString());
            binding.txtCategoryLevel1.setTextColor(getResources().getColor(R.color.primary, null));
        } else {
            binding.txtCategoryLevel1.setText("Chọn phân loại con");
            binding.txtCategoryLevel1.setTextColor(getResources().getColor(R.color.tertiary, null));
        }

        setupSubcategories(product.getCategory(), allProducts);

        // Images
        selectedImages.clear();
        if (product.getAlbum() != null) {
            selectedImages.addAll(product.getAlbum());
        }
        renderImages();

        // Ingredients Image
        ingredientsImageUrl = product.getIngredientsImage() != null ? product.getIngredientsImage() : "";
        if (!ingredientsImageUrl.isEmpty()) {
            binding.imvIngredientsImage.setVisibility(View.VISIBLE);
            ImageUtils.loadImage(requireContext(), binding.imvIngredientsImage, ingredientsImageUrl, R.color.gray_light);
        }

        // Usage Media
        usageMediaUrl = product.getUsageMedia() != null ? product.getUsageMedia() : "";
        if (!usageMediaUrl.isEmpty()) {
            binding.imvUsageMedia.setVisibility(View.VISIBLE);
            ImageUtils.loadImage(requireContext(), binding.imvUsageMedia, usageMediaUrl, R.color.gray_light);
        }

        // Key Ingredients
        binding.layoutKeyIngredientsContainer.removeAllViews();
        if (product.getKeyIngredients() != null) {
            for (KeyIngredient ingredient : product.getKeyIngredients()) {
                addKeyIngredientRow(ingredient.getName(), ingredient.getDescription());
            }
        }

        binding.edtDescription.setText(product.getDescription());
        binding.edtSuitableFor.setText(product.getSuitableFor());
        binding.edtMainIngredients.setText(product.getMainIngredientsSummary());
        binding.edtAllergyInfo.setText(product.getAllergyInformation());
        binding.edtStoryDescription.setText(product.getStoryDescription());

        if (product.getBenefits() != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < product.getBenefits().size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(product.getBenefits().get(i));
            }
            binding.edtBenefits.setText(sb.toString());
        }

        if (product.getDetailedIngredients() != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < product.getDetailedIngredients().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(product.getDetailedIngredients().get(i));
            }
            binding.edtDetailedIngredients.setText(sb.toString());
        }

        binding.edtTexture.setText(product.getTexture());
        binding.edtScent.setText(product.getScent());
        binding.edtNotes.setText(product.getNotes());
        binding.edtOrigin.setText(product.getOrigin());
        binding.edtUsageAmount.setText(product.getUsageAmount());
        binding.edtUsage.setText(product.getUsage());
    }

    private void setupCategoriesSpinner() {
        setupCategoriesSpinner(viewModel.getAllProducts().getValue() != null ? viewModel.getAllProducts().getValue() : new ArrayList<>());
    }

    private void setupCategoriesSpinner(List<ProductEntity> allProducts) {
        Set<String> categorySet = new HashSet<>();
        for (ProductEntity product : allProducts) {
            String cat = product.getCategory();
            if (cat != null && !cat.isEmpty() && !cat.equalsIgnoreCase("Chăm sóc tóc")) {
                categorySet.add(cat);
            }
        }
        List<String> categories = new ArrayList<>(categorySet);

        // Add defaults if empty
        if (categories.isEmpty()) {
            categories.addAll(Arrays.asList("Chăm sóc da", "Combo & Bộ Sản Phẩm"));
        }

        // Set click listener for Category Level 0 Bottom Sheet
        binding.layoutCategoryLevel0.setOnClickListener(v -> {
            ProductGenericBottomSheetFragment bottomSheet = ProductGenericBottomSheetFragment.newSingleSelectInstance(
                    "Chọn danh mục",
                    categories,
                    selectedCategoryName,
                    index -> {
                        String selectedCat = categories.get(index);
                        selectedCategoryName = selectedCat;
                        binding.txtCategoryLevel0.setText(selectedCat);
                        binding.txtCategoryLevel0.setTextColor(getResources().getColor(R.color.primary, null));

                        // Reset subcategory selection
                        selectedSubcategoriesSet.clear();
                        binding.txtCategoryLevel1.setText("Chọn phân loại con");
                        binding.txtCategoryLevel1.setTextColor(getResources().getColor(R.color.tertiary, null));

                        setupSubcategories(selectedCat, allProducts);
                    }
            );
            bottomSheet.show(getChildFragmentManager(), "CategoryLevel0BottomSheet");
        });
    }

    private void showImageSourceDialog(Runnable onGallerySelected, Runnable onCameraSelected) {
        List<String> options = Arrays.asList("Chọn từ thư viện ảnh (Album)", "Chụp ảnh mới (Máy ảnh)");
        ProductGenericBottomSheetFragment bottomSheet = ProductGenericBottomSheetFragment.newSingleSelectInstance(
                "Chọn nguồn hình ảnh",
                options,
                "",
                index -> {
                    if (index == 0) {
                        onGallerySelected.run();
                    } else if (index == 1) {
                        onCameraSelected.run();
                    }
                }
        );
        bottomSheet.show(getChildFragmentManager(), "ImageSourceBottomSheet");
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File file = new File(requireContext().getCacheDir(), "temp_img_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return Uri.EMPTY;
        }
    }

    private void addKeyIngredientRow(String name, String desc) {
        View rowView = getLayoutInflater().inflate(R.layout.item_key_ingredient_row, binding.layoutKeyIngredientsContainer, false);
        EditText edtName = rowView.findViewById(R.id.edt_ingredient_name);
        EditText edtDesc = rowView.findViewById(R.id.edt_ingredient_desc);
        View btnDelete = rowView.findViewById(R.id.btn_delete_ingredient);

        edtName.setText(name);
        edtDesc.setText(desc);

        btnDelete.setOnClickListener(v -> binding.layoutKeyIngredientsContainer.removeView(rowView));

        binding.layoutKeyIngredientsContainer.addView(rowView);
    }

    private void renderImages() {
        binding.layoutImagesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < selectedImages.size(); i++) {
            final int index = i;
            String imageSource = selectedImages.get(i);
            View itemView = inflater.inflate(R.layout.item_add_product_image, binding.layoutImagesContainer, false);
            com.google.android.material.imageview.ShapeableImageView imvPhoto = itemView.findViewById(R.id.imv_photo);
            ImageView btnDelete = itemView.findViewById(R.id.btn_delete);

            ImageUtils.loadImage(requireContext(), imvPhoto, imageSource, R.color.gray_light);

            btnDelete.setOnClickListener(v -> {
                selectedImages.remove(index);
                renderImages();
            });

            binding.layoutImagesContainer.addView(itemView);
        }

        // Always show the Add Image button
        binding.btnAddImage.setVisibility(View.VISIBLE);
    }

    private void saveProduct() {
        String name = binding.edtProductName.getText().toString().trim();
        String sku = binding.edtSku.getText().toString().trim();
        String stockStr = binding.edtStock.getText().toString().trim();
        String priceStr = binding.edtPrice.getText().toString().trim();
        String originalPriceStr = binding.edtOriginalPrice.getText().toString().trim();

        if (name.isEmpty()) {
            binding.edtProductName.setError("Tên sản phẩm bắt buộc");
            return;
        }
        if (sku.isEmpty()) {
            binding.edtSku.setError("SKU bắt buộc");
            return;
        }
        if (stockStr.isEmpty()) {
            binding.edtStock.setError("Số lượng bắt buộc");
            return;
        }
        if (priceStr.isEmpty()) {
            binding.edtPrice.setError("Giá bán bắt buộc");
            return;
        }

        int stock = 0;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (Exception ignored) {}

        long price = 0L;
        try {
            price = Long.parseLong(priceStr);
        } catch (Exception ignored) {}

        Long originalPrice = null;
        try {
            if (!originalPriceStr.isEmpty()) {
                originalPrice = Long.parseLong(originalPriceStr);
            }
        } catch (Exception ignored) {}

        String category = selectedCategoryName;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String sub : selectedSubcategoriesSet) {
            if (count > 0) sb.append(",");
            sb.append(sub);
            count++;
        }
        String subcategory = sb.toString();

        if (category.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn danh mục chính", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = binding.edtDescription.getText().toString().trim();
        String suitableFor = binding.edtSuitableFor.getText().toString().trim();
        String mainIngredients = binding.edtMainIngredients.getText().toString().trim();
        String allergyInfo = binding.edtAllergyInfo.getText().toString().trim();
        String storyDescription = binding.edtStoryDescription.getText().toString().trim();

        // Extract Dynamic Key Ingredients List
        List<KeyIngredient> keyIngredients = new ArrayList<>();
        for (int i = 0; i < binding.layoutKeyIngredientsContainer.getChildCount(); i++) {
            View row = binding.layoutKeyIngredientsContainer.getChildAt(i);
            EditText rowNameEdit = row.findViewById(R.id.edt_ingredient_name);
            EditText rowDescEdit = row.findViewById(R.id.edt_ingredient_desc);
            if (rowNameEdit != null && rowDescEdit != null) {
                String rowName = rowNameEdit.getText().toString().trim();
                String rowDesc = rowDescEdit.getText().toString().trim();
                if (!rowName.isEmpty() && !rowDesc.isEmpty()) {
                    KeyIngredient keyIngredient = new KeyIngredient();
                    keyIngredient.setName(rowName);
                    keyIngredient.setDescription(rowDesc);
                    keyIngredients.add(keyIngredient);
                }
            }
        }

        // Extract Benefits list (split by newline)
        String benefitsRaw = binding.edtBenefits.getText().toString().trim();
        List<String> benefits = new ArrayList<>();
        if (!benefitsRaw.isEmpty()) {
            String[] parts = benefitsRaw.split("\n");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    benefits.add(clean);
                }
            }
        }

        // Extract Detailed Ingredients list (split by newline or comma)
        String detailedIngredientsRaw = binding.edtDetailedIngredients.getText().toString().trim();
        List<String> detailedIngredients = new ArrayList<>();
        if (!detailedIngredientsRaw.isEmpty()) {
            String[] parts = detailedIngredientsRaw.split("[\n,]");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    detailedIngredients.add(clean);
                }
            }
        }

        String texture = binding.edtTexture.getText().toString().trim();
        String scent = binding.edtScent.getText().toString().trim();
        String notes = binding.edtNotes.getText().toString().trim();
        String origin = binding.edtOrigin.getText().toString().trim();
        String usageAmount = binding.edtUsageAmount.getText().toString().trim();
        String usage = binding.edtUsage.getText().toString().trim();

        String mainImage = selectedImages.isEmpty() ? "" : selectedImages.get(0);

        String productId = isEditMode ? editingProductId : UUID.randomUUID().toString();

        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setName(name);
        product.setSku(sku);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setStock(stock);
        product.setDescription(description);
        product.setMainImage(mainImage);
        product.setAlbum(selectedImages);
        product.setSuitableFor(suitableFor);
        product.setMainIngredientsSummary(mainIngredients);
        product.setAllergyInformation(allergyInfo);
        product.setStoryDescription(storyDescription);
        product.setStoryImage("");
        product.setIngredientsImage(ingredientsImageUrl);
        product.setKeyIngredients(keyIngredients);
        product.setBenefits(benefits);
        product.setTexture(texture);
        product.setScent(scent);
        product.setNotes(notes);
        product.setOrigin(origin);
        product.setUsageAmount(usageAmount);
        product.setUsage(usage);
        product.setUsageMedia(usageMediaUrl);
        product.setDetailedIngredients(detailedIngredients);

        // Save using ViewModel
        viewModel.saveProduct(product);
        String successMsg = isEditMode ? "Cập nhật sản phẩm thành công" : "Thêm sản phẩm thành công";
        Toast.makeText(getContext(), successMsg, Toast.LENGTH_SHORT).show();

        // Return to product list
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
